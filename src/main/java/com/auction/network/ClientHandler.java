package com.auction.network;

import com.auction.model.User;
import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.service.AuctionService;
import com.auction.model.Observer;
import com.auction.service.UserManager; // Import UserManager Singleton
import com.auction.util.DataManager;
import com.google.gson.Gson;
import com.auction.exception.AuthenticationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, Observer {

    private static final int REQ_REGISTER = 4; // REGISTER|user|pass|role
    private static final int REQ_LOGIN = 3; // LOGIN|user|pass
    private static final int REQ_BID = 3; // BID|auctionId|amount
    private static final int REQ_CREATE = 5; // CREATE_AUCTION|type|name|price|min
    private static final int REQ_END = 2; // END_AUCTION|auctionId
    private static final int REQ_HISTORY = 2; // GET_HISTORY|auctionId
    private static final int REQ_DEPOSIT = 2; // DEPOSIT|amount
    private static final int REQ_GET_BALANCE = 1; // GET_BALANCE
    private static final int REQ_WATCH = 2; // WATCH|auctionId
    private static final int REQ_AUTO_BID = 3; // ADD_AUTO_BID|auctionId|maxBid
    private static final int REQ_UNWATCH = 2; // UNWATCH|auctionId
    private static final int REQ_GET_WATCHLIST = 1; // GET_WATCHLIST

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc,
                            context) -> new com.google.gson.JsonPrimitive(src.toString()))
            .create();
    private User currentUser;

    public ClientHandler(final Socket socket) {
        this.socket = socket;
    }

    /**
     * Hàm Helper kiểm tra độ dài Payload để chống ArrayIndexOutOfBoundsException.
     *
     */
    private boolean validatePayload(String[] parts, int expectedLength) {
        if (parts == null || parts.length < expectedLength) {
            sendMessage("ERROR|Thiếu tham số yêu cầu. Cần ít nhất " + expectedLength + " phần.");
            return false;
        }
        return true;
    }

    @Override
    public final void run() {
        AuctionService auctionService = AuctionService.getInstance();
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.trim().split("\\|");
                // FIX [NeedBraces]: thêm {} cho if một dòng
                if (parts.length == 0) {
                    continue;
                }
                String cmd = parts[0];

                switch (cmd) {
                    case Protocol.CMD_REGISTER:
                        // FIX [NeedBraces]: thêm {} cho if (validatePayload(...))
                        if (validatePayload(parts, REQ_REGISTER)) {
                            handleRegister(parts);
                        }
                        break;
                    case Protocol.CMD_LOGIN:
                        if (validatePayload(parts, REQ_LOGIN)) {
                            handleLogin(parts, auctionService);
                        }
                        break;
                    case Protocol.CMD_LIST_AUCTIONS:
                        handleListAuctions(auctionService);
                        break;
                    case Protocol.CMD_BID:
                        if (validatePayload(parts, REQ_BID)) {
                            handleBid(parts, auctionService);
                        }
                        break;
                    case Protocol.CMD_CREATE_AUCTION:
                        if (validatePayload(parts, REQ_CREATE)) {
                            handleCreateAuction(parts, auctionService);
                        }
                        break;
                    case Protocol.CMD_END_AUCTION:
                        if (validatePayload(parts, REQ_END)) {
                            handleEndAuction(parts, auctionService);
                        }
                        break;
                    case Protocol.CMD_GET_HISTORY:
                        if (validatePayload(parts, REQ_HISTORY)) {
                            handleGetHistory(parts, auctionService);
                        }
                        break;
                    case Protocol.CMD_DEPOSIT:
                        if (validatePayload(parts, REQ_DEPOSIT)) {
                            handleDeposit(parts);
                        }
                        break;
                    case Protocol.CMD_GET_BALANCE:
                        if (validatePayload(parts, REQ_GET_BALANCE)) {
                            handleGetBalance();
                        }
                        break;
                    case Protocol.CMD_WATCH:
                        if (validatePayload(parts, REQ_WATCH)) {
                            handleWatch(parts, auctionService);
                        }
                        break;

                    case Protocol.CMD_UNWATCH:
                        if (validatePayload(parts, REQ_UNWATCH)) {
                            handleUnwatch(parts);
                        }
                        break;

                    case Protocol.CMD_GET_WATCHLIST:
                        if (validatePayload(parts, REQ_GET_WATCHLIST)) {
                            handleGetWatchlist(auctionService);
                        }
                        break;

                    case Protocol.CMD_ADD_AUTO_BID:
                        if (validatePayload(parts, REQ_AUTO_BID)) {
                            handleAddAutoBid(parts, auctionService);
                        }
                        break;
                    default:
                        sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Lệnh không hợp lệ");
                }
            }
        } catch (IOException e) {
            System.out.println("Một client đã ngắt kết nối.");
        } finally {
            cleanUp();
        }
    }

    private void handleRegister(final String[] parts) {
        // REGISTER|username|password|role
        boolean success = UserManager.getInstance().register(parts[1], parts[2], parts[3]);
        if (success) {
            sendMessage(Protocol.RES_REGISTER_SUCCESS + Protocol.SEPARATOR + "Đăng ký thành công.");
        } else {
            sendMessage(Protocol.RES_REGISTER_FAILED + Protocol.SEPARATOR + "Tên người dùng đã tồn tại.");
        }
    }

    private void handleLogin(final String[] parts, AuctionService auctionService) {
        // LOGIN|username|password
        try {
            User user = UserManager.getInstance().login(parts[1], parts[2]);
            if (user != null) {
                this.currentUser = user;
                for (Auction auction : auctionService.getAllAuctions()) {
                    auction.addObserver(this);
                }
                sendMessage(Protocol.RES_LOGIN_SUCCESS + Protocol.SEPARATOR + user.getRole() + Protocol.SEPARATOR
                        + "Chào " + user.getUsername());
            }
        } catch (AuthenticationException e) {
            sendMessage(Protocol.RES_LOGIN_FAILED + Protocol.SEPARATOR + e.getMessage());
        }
    }

    private void handleListAuctions(AuctionService auctionService) {
        // Trả về danh sách thô (Trong thực tế nên dùng JSON hoặc toString chuẩn)
        sendMessage(Protocol.RES_LIST_SUCCESS + Protocol.SEPARATOR + auctionService.getAllAuctions().toString());

    }

    private void handleCreateAuction(final String[] parts, AuctionService auctionService) {
        // CREATE_AUCTION|type|name|startingPrice|durationMinutes
        if (currentUser == null) {
            sendMessage(Protocol.ERROR + "|Bạn phải đăng nhập.");
            return;
        }

        // Chỉ ADMIN hoặc SELLER mới được tạo ()
        if (!"ADMIN".equals(currentUser.getRole()) && !"SELLER".equals(currentUser.getRole())) {
            sendMessage(Protocol.ERROR + "|Quyền hạn không đủ.");
            return;
        }
        try {
            String type = parts[1];
            String name = parts[2];
            double price = Double.parseDouble(parts[3]);
            long duration = Long.parseLong(parts[4]);
            String sellerId = currentUser.getUsername();

            auctionService.createNewAuction(type, name, price, duration, sellerId);
            sendMessage(Protocol.RES_SUCCESS + Protocol.SEPARATOR + "Sản phẩm " + name + " đã được đăng sàn.");
        } catch (Exception e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Dữ liệu tạo sản phẩm không hợp lệ.");
        }
    }

    private void handleEndAuction(final String[] parts, AuctionService auctionService) {
        // Kiểm tra quyền: Chỉ Admin mới được đóng phiên thủ công
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Chỉ Admin mới có quyền đóng phiên.");
            return;
        }
        auctionService.endAuction(parts[1]);
        sendMessage(Protocol.RES_END_SUCCESS + Protocol.SEPARATOR + "Đã đóng phiên " + parts[1]);
    }

    private void handleBid(final String[] parts, AuctionService auctionService) {
        if (this.currentUser == null) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn phải đăng nhập trước khi đấu giá!");
            return;
        }

        try {
            String auctionId = parts[1];
            double amount = Double.parseDouble(parts[2]);
            if (amount <= 0) {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá bid phải lớn hơn 0");
                return;
            }
            if (Double.isNaN(amount) || Double.isInfinite(amount)) {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền không hợp lệ (NaN/Infinite)");
                return;
            }

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá này");
                return;
            }

            auction.processNewBid(currentUser, amount);
            sendMessage(Protocol.RES_BID_SUCCESS + Protocol.SEPARATOR + auctionId + Protocol.SEPARATOR + amount);
            DataManager.getInstance().saveData();
        } catch (NumberFormatException e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền phải là con số hợp lệ");
        } catch (Exception e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + e.getMessage());
        }
    }

    // --- Xử lý nạp tiền ---
    private void handleDeposit(String[] parts) {
        if (currentUser == null) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Vui lòng đăng nhập để nạp tiền.");
            return;
        }
        try {
            double amount = Double.parseDouble(parts[1]);
            if (amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount)) {
                currentUser.addBalance(amount);

                // Lưu dữ liệu ngay lập tức để tránh mất tiền của khách
                DataManager.getInstance().saveData();

                sendMessage(Protocol.RES_DEPOSIT_SUCCESS + Protocol.SEPARATOR +
                        currentUser.getBalance() + Protocol.SEPARATOR +
                        "Đã nạp thành công: " + amount + "$");
            } else {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Số tiền nạp không hợp lệ.");
            }
        } catch (NumberFormatException e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá nạp phải là con số.");
        }
    }

    // --- Kiểm tra số dư ---
    private void handleGetBalance() {
        if (currentUser == null) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn chưa đăng nhập.");
            return;
        }
        // Trả về số dư hiện tại theo đúng Protocol
        sendMessage(Protocol.RES_BALANCE_INFO + Protocol.SEPARATOR + currentUser.getBalance());

    }

    /**
     * Xử lý yêu cầu lấy lịch sử giá của một phiên đấu giá.
     * Trả về danh sách BidTransaction dưới dạng JSON để FE vẽ biểu đồ.
     */
    private void handleGetHistory(String[] parts, AuctionService auctionService) {

        String auctionId = parts[1];
        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction != null) {
            try {
                String jsonHistory = gson.toJson(auction.getBidHistory());
                sendMessage(Protocol.RES_HISTORY + Protocol.SEPARATOR + auctionId + Protocol.SEPARATOR + jsonHistory);
            } catch (Exception e) {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Lỗi xử lý dữ liệu lịch sử: " + e.getMessage());
            }
        } else {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá với ID: " + auctionId);
        }

    }

    // --- LOGIC XỬ LÝ WATCHLIST ---
    private void handleWatch(String[] parts, AuctionService auctionService) {
        if (!(currentUser instanceof Bidder)) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Chỉ người mua mới có thể theo dõi sản phẩm.");
            return;
        }

        String auctionId = parts[1];
        boolean isSuccess = ((Bidder) currentUser).addToWatchlist(auctionId);
        if (isSuccess) {
            // Lưu dữ liệu để bảo toàn danh sách theo dõi
            DataManager.getInstance().saveData();

            sendMessage(Protocol.RES_WATCH_SUCCESS + Protocol.SEPARATOR + auctionId);
            Auction targetAuction = auctionService.getAuctionById(auctionId);
            if (targetAuction != null) {
                targetAuction.addObserver(this); // Đăng ký chính ClientHandler này để nhận tin nhắn
                System.out.println("[WATCHLIST] User " + currentUser.getUsername()
                        + " đã bắt đầu nhận thông báo từ phiên " + auctionId);
            }
        } else {
            // Thêm phản hồi nếu Watchlist đầy hoặc sản phẩm đã có sẵn
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR
                    + "Theo dõi thất bại! (Sản phẩm đã có trong danh sách hoặc không tồn tại)");
        }
    }

    private void handleUnwatch(String[] parts) {
        if (currentUser instanceof Bidder) {
            String auctionId = parts[1];
            ((Bidder) currentUser).removeFromWatchlist(auctionId);
            sendMessage("UNWATCH_SUCCESS|" + auctionId);
            DataManager.getInstance().saveData();
            sendMessage(Protocol.RES_UNWATCH_SUCCESS + Protocol.SEPARATOR + auctionId);
        }
    }

    private void handleGetWatchlist(AuctionService auctionService) {
        if (!(currentUser instanceof Bidder)) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Bạn chưa đăng nhập hoặc không phải bidder.");
            return;
        }

        List<Auction> watchlist = auctionService.getWatchlistForUser(currentUser.getUsername());
        String jsonWatchlist = gson.toJson(watchlist);

        sendMessage(Protocol.RES_WATCHLIST + Protocol.SEPARATOR + jsonWatchlist);
    }

    private void handleAddAutoBid(final String[] parts, AuctionService auctionService) {
        if (!(currentUser instanceof Bidder)) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Chỉ người mua mới có quyền cài đặt Robot.");
            return;
        }

        try {
            String auctionId = parts[1];
            double maxBid = Double.parseDouble(parts[2]); // Ngân sách tối đa của khách

            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá.");
                return;
            }

            auction.addAutoBidConfig(currentUser.getUsername(), maxBid);

            DataManager.getInstance().saveData();

            // Phản hồi cho FE để hiển thị thông báo
            sendMessage(Protocol.RES_AUTO_BID_SUCCESS + Protocol.SEPARATOR +
                    auctionId + Protocol.SEPARATOR + "Autobid bot đã sẵn sàng với hạn mức: " + (long) maxBid + " VNĐ");

            System.out.println(
                    "[AUTOBID] Người dùng " + currentUser.getUsername() + " đã kích hoạt Autobid cho phiên "
                            + auctionId);

        } catch (NumberFormatException e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Ngân sách tối đa phải là một con số.");
        } catch (Exception e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Lỗi hệ thống: " + e.getMessage());
        }
    }

    public final void sendMessage(final String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public final void update(final String msg) {
        this.sendMessage(msg);
    }

    private void cleanUp() {
        try {
            AuctionService.getInstance().removeObserverFromAll(this);

            // FIX [NeedBraces]: thêm {} cho if (in != null) và if (out != null)
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng tài nguyên Client: " + e.getMessage());
        }
    }
}
