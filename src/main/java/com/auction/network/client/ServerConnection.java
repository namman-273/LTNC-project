package com.auction.network.client;

import com.auction.network.protocol.Protocol;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Kết nối với ClientHandler.
 * - Tối ưu: 2 ReentrantLock (connectionLock + requestLock)
 * - Hỗ trợ: Đọc cấu hình server từ file server.properties
 */
public class ServerConnection {

  // ===== THÊM: Đọc từ file properties =====
  private static String DEFAULT_HOST = "localhost";
  private static int DEFAULT_PORT = 9999;

  private static final int MAX_RETRY = 3;
  private static final int RETRY_DELAY_MS = 1000;

  private String host;
  private int port;

  // Các biến mạng cần volatile để các luồng nhìn thấy trạng thái mới nhất
  private volatile Socket socket;
  private volatile PrintWriter out;
  private volatile BufferedReader in;
  private volatile boolean isListening = false;

  // --- KIẾN TRÚC 2 LOCK (CHỐNG NGHẼN CỔ CHAI) ---
  // Bảo vệ trạng thái đóng/mở kết nối (Rất ngắn)
  private final ReentrantLock connectionLock = new ReentrantLock();
  // Xếp hàng các luồng muốn gửi tin nhắn (Bị block 5s cũng không ảnh hưởng luồng
  // khác)
  private final ReentrantLock requestLock = new ReentrantLock();

  private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
  private final List<Consumer<String>> pushListeners = new CopyOnWriteArrayList<>();

  private static volatile ServerConnection instance;

  private ServerConnection() {
    loadServerConfig();
    this.host = DEFAULT_HOST;
    this.port = DEFAULT_PORT;
  }

  /**
   * Đọc cấu hình server từ file server.properties.
   */
  private void loadServerConfig() {
    try {
      File configFile = new File("server.properties");

      if (configFile.exists()) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
          props.load(fis);
          DEFAULT_HOST = props.getProperty("server.host", "localhost");
          String portStr = props.getProperty("server.port", "9999");
          try {
            DEFAULT_PORT = Integer.parseInt(portStr);
          } catch (NumberFormatException e) {
            DEFAULT_PORT = 9999;
            System.out.println("⚠ Port không hợp lệ, dùng 9999");
          }
          System.out.println("✓ Đã load config từ server.properties"
              + "  → Server: " + DEFAULT_HOST + ":" + DEFAULT_PORT);
        }
      } else {
        System.out.println("Không tìm thấy server.properties"
            + "→ Sẽ dùng: localhost:9999 (mặc định)");
      }
    } catch (Exception e) {
      System.out.println("⚠ Lỗi đọc config: " + e.getMessage()
          + "  → Sẽ dùng: localhost:9999 (mặc định)");
    }
  }

  /**
   * Singleton.
   */
  public static ServerConnection getInstance() {
    if (instance == null) {
      synchronized (ServerConnection.class) {
        if (instance == null) {
          instance = new ServerConnection();
        }
      }
    }
    return instance;
  }

  // ─── 1. QUẢN LÝ KẾT NỐI (Dùng connectionLock) ─────────────────────────

  /**
   * Kết nối.
   */
  public boolean connect() {
    connectionLock.lock();
    try {
      if (isConnected()) {
        return true;
      }

      closeQuietly(); // Dọn dẹp an toàn trước khi tạo mới

      socket = new Socket(host, port);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      startInternalListener();
      System.out.println("✓ Kết nối server thành công: " + host + ":" + port);
      return true;
    } catch (Exception e) {
      System.err.println("✗ Không thể kết nối " + host + ":" + port);
      System.err.println("  Lỗi: " + e.getMessage());
      closeQuietly();
      return false;
    } finally {
      connectionLock.unlock();
    }
  }

  /**
   * Kết nối lại.
   */
  public boolean connectWithRetry() {
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      System.out.println("Đang kết nối server... (lần " + attempt + "/" + MAX_RETRY + ")");
      if (connect()) {
        return true;
      }
      if (attempt < MAX_RETRY) {
        try {
          Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }
    System.err.println("Không thể kết nối sau " + MAX_RETRY + " lần thử!");
    return false;
  }

  /**
   * Bỏ kết nối.
   */
  public void disconnect() {
    connectionLock.lock();
    try {
      isListening = false;
      closeQuietly();
      responseQueue.clear();
      System.out.println("Đã ngắt kết nối an toàn.");
    } finally {
      connectionLock.unlock();
    }
  }

  private void closeQuietly() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
    socket = null;
    out = null;
    in = null;
  }

  public boolean isConnected() {
    Socket s = socket;
    return s != null && !s.isClosed() && s.isConnected() && isListening;
  }

  // ─── 2. GIAO TIẾP MẠNG (Dùng requestLock) ─────────────────────────────
  /**
   * Gửi và nhận đồng bộ (Chỉ có requestLock, không hold connectionLock).
   */
  public String sendAndReceive(String message) {
    // Nếu mất kết nối thì thử connect ngay từ đầu
    if (!isConnected() && !connectWithRetry()) {
      return Protocol.ERROR + Protocol.SEPARATOR + "Không thể kết nối server!";
    }

    requestLock.lock();
    try {
      // Dọn đúng 1 lần duy nhất ở đây
      responseQueue.clear();
      return doSendAndReceive(message, true);
    } finally {
      requestLock.unlock();
    }
  }

  private String doSendAndReceive(String message, boolean allowRetry) {
    PrintWriter localOut = this.out;
    if (localOut == null || !isConnected()) {
      return Protocol.ERROR + Protocol.SEPARATOR + "Mất kết nối!";
    }

    localOut.println(message);

    try {
      String response = responseQueue.poll(5, TimeUnit.SECONDS);
      if (response != null) {
        return response;
      }

      // Nếu không cho phép thử lại nữa
      if (!allowRetry) {
        return Protocol.ERROR + Protocol.SEPARATOR + "Server không phản hồi sau khi kết nối lại!";
      }

      // Nếu Timeout -> Thử kết nối lại và gửi đệ quy 1 lần duy nhất
      System.out.println("Timeout 5s, đang thử kết nối lại...");
      if (!connectWithRetry()) {
        return Protocol.ERROR + Protocol.SEPARATOR + "Mất kết nối server!";
      }

      // Gọi lại với allowRetry = false
      return doSendAndReceive(message, false);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Protocol.ERROR + Protocol.SEPARATOR + "Luồng bị gián đoạn!";
    }
  }

  // ─── 3. LISTENER THREAD (Lock-free) ────────────────────────────────────

  private void startInternalListener() {
    if (isListening) {
      return;
    }

    isListening = true;
    final Socket currentSocket = this.socket;
    final BufferedReader currentIn = this.in;

    Thread listenerThread = new Thread(() -> {
      try {
        String line;
        while (isListening && (line = currentIn.readLine()) != null) {
          if (isPushMessage(line)) {
            for (Consumer<String> listener : pushListeners) {
              try {
                listener.accept(line);
              } catch (Exception ignored) {
                ignored.printStackTrace();
              }
            }
          } else {
            responseQueue.offer(line);
          }
        }
      } catch (Exception e) {
        if (isListening) {
          System.err.println("Mất kết nối Thread lắng nghe.");
        }
      } finally {
        // Dọn dẹp với connectionLock
        connectionLock.lock();
        try {
          if (this.socket == currentSocket) {
            isListening = false;
            closeQuietly();
            System.out.println("Đã dọn dẹp Socket cũ an toàn.");
          }
        } finally {
          connectionLock.unlock();
        }
      }
    });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private boolean isPushMessage(String line) {
    if (line == null || line.isEmpty()) {
      return false;
    }
    String[] parts = line.split("\\|");
    String header = parts[0];

    return header.equals(Protocol.NOTI_BID_UPDATE)
        || header.equals(Protocol.NOTI_SNIPING_UPDATE)
        || header.equals(Protocol.NOTI_BALANCE_CHANGED)
        || header.equals(Protocol.NOTI_NEW_AUCTION)
        || header.equals(Protocol.NOTI_OUTBID)
        || header.equals(Protocol.NOTI_REFUND)
        || header.equals(Protocol.NOTI_AUCTION_CANCELLED)
        || header.equals(Protocol.RES_END_SUCCESS);
  }

  /**
   * Cài nghe tin push.
   */
  public void addPushListener(Consumer<String> listener) {
    if (listener != null && !pushListeners.contains(listener)) {
      pushListeners.add(listener);
    }
  }

  public void removePushListener(Consumer<String> listener) {
    pushListeners.remove(listener);
  }
}