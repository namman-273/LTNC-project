package com.auction.network;

import com.auction.model.User;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.service.AuctionService;
import com.auction.model.Observer;
import com.auction.service.UserManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.auction.exception.AuthenticationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientHandler implements Runnable, Observer {

    private static final int REQ_REGISTER = 4; // REGISTER|user|pass|role
    private static final int REQ_LOGIN    = 3; // LOGIN|user|pass
    private static final int REQ_BID      = 3; // BID|auctionId|amount
    private static final int REQ_CREATE   = 5; // CREATE_AUCTION|type|name|price|min
    private static final int REQ_END      = 2; // END_AUCTION|auctionId
    private static final int REQ_HISTORY  = 2; // GET_HISTORY|auctionId

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Gson dùng chung — serialize LocalDateTime thành chuỗi ISO để FE đọc được
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .create();

    private User currentUser;

    public ClientHandler(final Socket socket) {
        this.socket = socket;
    }

    // DTO nhỏ gọn để trả về danh sách phiên — chỉ chứa các field FE cần
    // Không dùng class Auction thô vì Gson sẽ cố serialize cả lock, observer, executor...
    private static class AuctionDTO {
        String id;
        String itemName;
        double currentPrice;
        String status;
        long endTime;

        AuctionDTO(Auction a) {
            this.id           = a.getId();
            this.itemName     = a.getItem() != null ? a.getItem().getItemName() : "---";
            this.currentPrice = a.getCurrentPrice();
            this.status       = a.getStatus().name();
            this.endTime      = a.getEndTime();
        }
    }

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
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while ((request = in.readLine()) != null) {
                String[] parts = request.trim().split("\\|");
                if (parts.length == 0) continue;
                String cmd = parts[0];

                switch (cmd) {
                    case Protocol.CMD_REGISTER:
                        if (validatePayload(parts, REQ_REGISTER))
                            handleRegister(parts);
                        break;
                    case Protocol.CMD_LOGIN:
                        if (validatePayload(parts, REQ_LOGIN))
                            handleLogin(parts, auctionService);
                        break;
                    case Protocol.CMD_LIST_AUCTIONS:
                        handleListAuctions(auctionService);
                        break;
                    case Protocol.CMD_BID:
                        if (validatePayload(parts, REQ_BID))
                            handleBid(parts, auctionService);
                        break;
                    case Protocol.CMD_CREATE_AUCTION:
                        if (validatePayload(parts, REQ_CREATE))
                            handleCreateAuction(parts, auctionService);
                        break;
                    case Protocol.CMD_END_AUCTION:
                        if (validatePayload(parts, REQ_END))
                            handleEndAuction(parts, auctionService);
                        break;
                    case Protocol.CMD_GET_HISTORY:
                        if (validatePayload(parts, REQ_HISTORY))
                            handleGetHistory(parts, auctionService);
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
        boolean success = UserManager.getInstance().register(parts[1], parts[2], parts[3]);
        if (success) {
            sendMessage(Protocol.RES_REGISTER_SUCCESS + Protocol.SEPARATOR + "Đăng ký thành công.");
        } else {
            sendMessage(Protocol.RES_REGISTER_FAILED + Protocol.SEPARATOR + "Tên người dùng đã tồn tại.");
        }
    }

    private void handleLogin(final String[] parts, AuctionService auctionService) {
        try {
            User user = UserManager.getInstance().login(parts[1], parts[2]);
            if (user != null) {
                this.currentUser = user;
                for (Auction auction : auctionService.getAllAuctions()) {
                    auction.addObserver(this);
                }
                sendMessage(Protocol.RES_LOGIN_SUCCESS + Protocol.SEPARATOR
                        + user.getRole() + Protocol.SEPARATOR + "Chào " + user.getUsername());
            }
        } catch (AuthenticationException e) {
            sendMessage(Protocol.RES_LOGIN_FAILED + Protocol.SEPARATOR + e.getMessage());
        }
    }

    /**
     * FIX: Trả JSON thay vì toString().
     * Format cũ: LIST_AUCTIONS_SUCCESS | [id=AUC_123,itemName=Phone,...]  ← dễ vỡ
     * Format mới: LIST_AUCTIONS_SUCCESS | [{"id":"AUC_123","itemName":"Phone",...}]  ← ổn định
     *
     * Dùng AuctionDTO thay vì Auction thô để tránh Gson cố serialize lock/executor.
     */
    private void handleListAuctions(AuctionService auctionService) {
        Collection<Auction> auctions = auctionService.getAllAuctions();

        List<AuctionDTO> dtoList = new ArrayList<>();
        for (Auction a : auctions) {
            dtoList.add(new AuctionDTO(a));
        }

        String json = gson.toJson(dtoList);
        sendMessage(Protocol.RES_LIST_SUCCESS + Protocol.SEPARATOR + json);
    }

    private void handleCreateAuction(final String[] parts, AuctionService auctionService) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Quyền hạn không đủ.");
            return;
        }
        try {
            String type     = parts[1];
            String name     = parts[2];
            double price    = Double.parseDouble(parts[3]);
            long duration   = Long.parseLong(parts[4]);

            auctionService.createNewAuction(type, name, price, duration);
            sendMessage(Protocol.RES_SUCCESS + Protocol.SEPARATOR + "Sản phẩm " + name + " đã được đăng sàn.");
        } catch (Exception e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Dữ liệu tạo sản phẩm không hợp lệ.");
        }
    }

    private void handleEndAuction(final String[] parts, AuctionService auctionService) {
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
            double amount    = Double.parseDouble(parts[2]);

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
        } catch (NumberFormatException e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Giá tiền phải là con số hợp lệ");
        } catch (Exception e) {
            sendMessage(Protocol.ERROR + Protocol.SEPARATOR + e.getMessage());
        }
    }

    private void handleGetHistory(String[] parts, AuctionService auctionService) {
        String auctionId = parts[1];
        Auction auction  = auctionService.getAuctionById(auctionId);

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

    public final void sendMessage(final String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public final void update(final String msg) {
        this.sendMessage(msg);
    }

    private void cleanUp() {
        try {
            AuctionService.getInstance().removeObserverFromAll(this);
            if (in != null)  in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng tài nguyên Client: " + e.getMessage());
        }
    }
}
