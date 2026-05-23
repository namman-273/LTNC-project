package com.auction.network.client;

import com.auction.network.protocol.Protocol;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * .
 */
public class ServerConnection {

  private static final String HOST = "localhost";
  private static final int PORT = 9999;
  private static final int MAX_RETRY = 3;
  private static final int RETRY_DELAY_MS = 1000;

  private String host;
  private int port;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  // --- FIX 1 & 2: CƠ CHẾ TÁCH BIỆT TIN NHẮN ---
  // Queue này giữ các phản hồi (ví dụ: LOGIN_SUCCESS, BID_FAILED...)
  private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
  // Listener này xử lý các tin Real-time (ví dụ: BID_UPDATE, SNIPING...)
  // Sử dụng CopyOnWriteArrayList để tránh lỗi khi vừa duyệt vừa xóa listener
  private final List<Consumer<String>> pushListeners = new CopyOnWriteArrayList<>();
  private volatile boolean isListening = false;

  private static volatile ServerConnection instance;

  private ServerConnection() {
    this.host = HOST;
    this.port = PORT;
  }

  /**
   * singleton.
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

  public boolean connect() {
    synchronized (this) {
      try {
        if (socket != null && !socket.isClosed() && socket.isConnected()) {
          return true;
        }
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Kích hoạt Thread lắng nghe ngay khi kết nối thành công
        startInternalListener();
        System.out.println("Kết nối server thành công!");
        return true;
      } catch (Exception e) {
        System.err.println("Không thể kết nối server: " + e.getMessage());
        socket = null;
        return false;
      }
    }
  }

  // --- HÀM QUAN TRỌNG: ĐỌC DỮ LIỆU TẬP TRUNG ---
  private void startInternalListener() {
    if (isListening) {
      return;
    }
    // Lưu lại tham chiếu socket hiện tại vào một biến cục bộ
    isListening = true;
    final Socket currentSocket = this.socket;

    Thread listenerThread = new Thread(() -> {
      try {
        String line;
        while (isListening && (line = in.readLine()) != null) {
          // Kiểm tra xem là tin nhắn Real-time (Push) hay Phản hồi lệnh (Response)
          if (isPushMessage(line)) {
            // Duyệt qua danh sách để phát sóng cho tất cả các màn hình đã đăng ký
            for (Consumer<String> listener : pushListeners) {
              listener.accept(line);
            }
          } else {
            responseQueue.offer(line); // Đẩy vào hàng đợi cho sendAndReceive lấy
          }
        }
      } catch (Exception e) {
        System.err.println("Mất kết nối Thread lắng nghe.");
      } finally {
        // CHỈ DỌN DẸP NẾU SOCKET VẪN LÀ CÁI CŨ
        synchronized (this) {
          if (this.socket == currentSocket) {
            isListening = false;
            try {
              if (socket != null && !socket.isClosed()) {
                socket.close();
              }
            } catch (Exception ex) {
            }
            socket = null;
            System.out.println("Đã dọn dẹp Socket cũ an toàn.");
          } else {
            System.out.println("Phát hiện Socket đã được thay mới, không xóa nhầm.");
          }
        }
      }
    });
    listenerThread.setDaemon(true); // Tự tắt khi App đóng
    listenerThread.start();
  }

  /**
   * Bộ lọc nhận diện tất cả các tin nhắn mang tính chất THÔNG BÁO (PUSH).
   * Đặc điểm: Đây là các tin nhắn Server tự gửi xuống mà không cần Client phải
   * gọi lệnh ngay lúc đó, hoặc gửi cho nhiều người cùng lúc qua Observer Pattern.
   * - NOTI_OUTBID: Thông báo riêng cho người bị vượt giá
   * - NOTI_REFUND: Thông báo khi tiền được hoàn lại
   * - NOTI_AUCTION_CANCELLED: Thông báo phiên đấu giá bị hủy bởi Admin
   * 
   * @param line Message từ server
   * 
   * @return true nếu là push notification, false nếu là response
   */
  private boolean isPushMessage(String line) {
    if (line == null || line.isEmpty()) {
      return false;
    }

    // Lấy Header (Phần trước dấu |)
    String header = line.split("\\|")[0];

    return
    // --- NHÓM 1: NOTIFICATIONS CHUNG ---
    // Thông báo về thay đổi giá đấu (gửi cho watchers + bidders)
    header.equals(Protocol.NOTI_BID_UPDATE)
        ||
        // Thông báo về gia hạn thời gian (Anti-sniping)
        header.equals(Protocol.NOTI_SNIPING_UPDATE)
        ||
        // Thông báo về thay đổi số dư ví
        header.equals(Protocol.NOTI_BALANCE_CHANGED)
        ||
        // Thông báo về auction mới được tạo
        header.equals(Protocol.NOTI_NEW_AUCTION)
        ||

        // --- NHÓM 2: NOTIFICATIONS CÁ NHÂN ---
        // Thông báo riêng cho người bị vượt giá
        header.equals(Protocol.NOTI_OUTBID)
        ||
        // Thông báo khi tiền được hoàn lại vào ví
        header.equals(Protocol.NOTI_REFUND)
        ||
        // Thông báo phiên đấu giá bị hủy bởi Admin
        header.equals(Protocol.NOTI_AUCTION_CANCELLED)
        ||

        // --- NHÓM 3: KẾT THÚC PHIÊN ĐẤU GIÁ ---
        // Khi một phiên kết thúc, Server dùng notify để báo cho TOÀN BỘ người đang xem
        header.equals(Protocol.RES_END_SUCCESS);
  }

  /**
   * Đăng ký listener để nhận push notifications.
   * Listener sẽ được gọi mỗi khi có message từ server thuộc loại push.
   * 
   * @param listener Consumer xử lý notification message
   */
  public void addPushListener(Consumer<String> listener) {
    if (listener != null && !pushListeners.contains(listener)) {
      pushListeners.add(listener);
    }
  }

  public void removePushListener(Consumer<String> listener) {
    pushListeners.remove(listener);
  }

  /**
   * Kết nối với retry tự động — thử lại MAX_RETRY lần nếu thất bại.
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

  public synchronized String sendAndReceive(String message) {
    // Thử gửi, nếu mất kết nối thì retry 1 lần
    try {
      if (!isConnected() && !connectWithRetry()) {
        System.out.println("Mất kết nối, đang thử kết nối lại...");
        if (!connectWithRetry()) {
          return "ERROR|Không thể kết nối server sau nhiều lần thử!";
        }
      }
      responseQueue.clear();
      out.println(message);

      // Đợi tối đa 5 giây để lấy đúng phản hồi của lệnh này
      String response = responseQueue.poll(5, TimeUnit.SECONDS);

      if (response == null) {
        throw new Exception("Server không phản hồi");
      }

      return response;

    } catch (Exception e) {
      System.err.println("Lỗi gửi/nhận: " + e.getMessage());

      // Retry 1 lần sau khi mất kết nối
      System.out.println("Đang thử kết nối lại...");
      try {
        if (connectWithRetry()) {
          responseQueue.clear();
          out.println(message);
          String retryResponse = responseQueue.poll(5, TimeUnit.SECONDS);
          return retryResponse != null
                    ? retryResponse
                    : "ERROR|Server không phản hồi sau khi kết nối lại!";
        }
      } catch (Exception retryEx) {
        System.err.println("Retry thất bại: " + retryEx.getMessage());
      }
      return "ERROR|Mất kết nối server!";
    }

  }

  public boolean isConnected() {
    return socket != null && !socket.isClosed() && socket.isConnected();
  }

  public synchronized void disconnect() {
    isListening = false; // báo listener thread dừng lại
    try {
      if (socket != null && !socket.isClosed())
        socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      socket = null;
      out = null;
      in = null;
    }
    instance = null;
  }

}