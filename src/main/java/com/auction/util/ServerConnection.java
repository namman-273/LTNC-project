package com.auction.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.auction.network.Protocol;

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

    private String host;
    private int port;

  // --- FIX 1 & 2: CƠ CHẾ TÁCH BIỆT TIN NHẮN ---
  // Queue này giữ các phản hồi (ví dụ: LOGIN_SUCCESS, BID_FAILED...)
  private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
  // Listener này xử lý các tin Real-time (ví dụ: BID_UPDATE, SNIPING...)
  private Consumer<String> pushListener;
  private boolean isListening = false;

  private static volatile ServerConnection instance;

  private ServerConnection() {
    this.host = HOST;
    this.port = PORT;
  }

  public ServerConnection(String host, int port) {
    this.host = host;
    this.port = port;
  }

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
    isListening = true;

    Thread listenerThread = new Thread(() -> {
      try {
        String line;
        while (isListening && (line = in.readLine()) != null) {
          // Kiểm tra xem là tin nhắn Real-time (Push) hay Phản hồi lệnh (Response)
          if (isPushMessage(line)) {
            if (pushListener != null) {
              pushListener.accept(line); // Đẩy cho UI xử lý
            }
          } else {
            responseQueue.offer(line); // Đẩy vào hàng đợi cho sendAndReceive lấy
          }
        }
      } catch (Exception e) {
        System.err.println("Mất kết nối Thread lắng nghe.");
      } finally {
        isListening = false;
        socket = null;
      }
    });
    listenerThread.setDaemon(true); // Tự tắt khi App đóng
    listenerThread.start();
  }

  // Hàm nhận diện tin nhắn Real-time (Leader có thể thêm các đầu mục Protocol vào
  // đây)
  /**
   * Bộ lọc này nhận diện tất cả các tin nhắn mang tính chất THÔNG BÁO (PUSH).
   * Đặc điểm: Đây là các tin nhắn Server tự gửi xuống mà không cần Client phải
   * gọi lệnh ngay lúc đó, hoặc gửi cho nhiều người cùng lúc qua Observer Pattern.
   */
  private boolean isPushMessage(String line) {
    if (line == null || line.isEmpty()) {
      return false;
    }

    // Lấy Header (Phần trước dấu |)
    String header = line.split("\\|")[0];

    return
    // --- NHÓM 1: CÁC NOTI MẶC ĐỊNH ---
    header.equals(Protocol.NOTI_BID_UPDATE)
        || // Giá nhảy (Real-time)
        header.equals(Protocol.NOTI_SNIPING_UPDATE)
        || // Gia hạn thời gian
        header.equals(Protocol.NOTI_BALANCE_CHANGED)
        || // Ví tiền biến động

        // --- NHÓM 2: CÁC LỆNH KẾT THÚC / THÀNH CÔNG GỬI QUA OBSERVER ---
        // Khi một phiên kết thúc, Server dùng notify để báo cho TOÀN BỘ người đang xem
        header.equals(Protocol.RES_END_SUCCESS)
        ||
        header.equals(Protocol.RES_SUCCESS)
        ||

        // --- NHÓM 3: CÁC TRƯỜNG HỢP LỖI HỆ THỐNG GỬI NGẦM ---
        // Ví dụ: Server sắp bảo trì hoặc lỗi logic tự động
        header.equals(Protocol.ERROR);
  }

  // Đăng ký để UI nhận tin Real-time
  public void setPushListener(Consumer<String> listener) {
    this.pushListener = listener;
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
      // Điều này giải quyết triệt để việc "ăn nhầm" tin UPDATE_PRICE
      String response = responseQueue.poll(5, TimeUnit.SECONDS);

      if (response == null) {
        throw new Exception("Server đóng kết nối đột ngột");
      }

      return response;

    } catch (Exception e) {
      System.err.println("Lỗi gửi/nhận: " + e.getMessage());
      socket = null;

      // Retry 1 lần sau khi mất kết nối
      System.out.println("Đang thử kết nối lại...");
      try {
        if (connectWithRetry()) {
          out.println(message);
          return responseQueue.poll(5, TimeUnit.SECONDS);
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

  public void disconnect() {
    synchronized (ServerConnection.class) {
      try {
        if (socket != null)
          socket.close();
        socket = null;
        instance = null;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
