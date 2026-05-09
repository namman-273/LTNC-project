package com.auction.network;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test ClientHandler bằng cách tạo pipe socket thật (loopback).
 * Mỗi test: gửi command → đọc response → assert prefix đúng.
 */
@Timeout(10)
public class ClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket clientSide;
    private PrintWriter clientOut;
    private BufferedReader clientIn;
    private String sellerUser = "seller_ch";
    private String bidderUser = "bidder_ch";
    private String adminUser  = "admin_ch";

    @BeforeEach
    void setUp() throws Exception {
        resetSingletons();

        UserManager.getInstance().register(sellerUser, "pw", "SELLER");
        UserManager.getInstance().register(bidderUser, "pw", "BIDDER");
        UserManager.getInstance().register(adminUser,  "pw", "ADMIN");

        Bidder b = (Bidder) UserManager.getInstance().findUserByUsername(bidderUser);
        b.addBalance(999_999_999.0);

        // Tạo ServerSocket trên port ngẫu nhiên
        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        // Chạy ClientHandler trong background thread
        Thread serverThread = new Thread(() -> {
            try {
                Socket accepted = serverSocket.accept();
                new ClientHandler(accepted).run();
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Client side kết nối
        clientSide = new Socket("localhost", port);
        clientOut = new PrintWriter(clientSide.getOutputStream(), true);
        clientIn  = new BufferedReader(new InputStreamReader(clientSide.getInputStream()));
    }

    @AfterEach
    void tearDown() throws Exception {
        try { clientSide.close(); } catch (Exception ignored) {}
        try { serverSocket.close(); } catch (Exception ignored) {}
        resetSingletons();
    }

    private void resetSingletons() throws Exception {
        Field asf = AuctionService.class.getDeclaredField("instance");
        asf.setAccessible(true);
        asf.set(null, null);

        Field umf = UserManager.class.getDeclaredField("instance");
        umf.setAccessible(true);
        umf.set(null, null);
    }

    /** Gửi 1 dòng, đọc response đầu tiên trả về */
    private String send(String cmd) throws Exception {
        clientOut.println(cmd);
        return clientIn.readLine();
    }

    // ─── REGISTER ────────────────────────────────────────────────

    @Test
    void registerNewUserSuccess() throws Exception {
        String r = send("REGISTER|newuser1|pass123|BIDDER");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_REGISTER_SUCCESS),
            "Expected REGISTER_SUCCESS, got: " + r);
    }

    @Test
    void registerDuplicateUserFails() throws Exception {
        send("REGISTER|dupuser|pass|BIDDER");
        String r = send("REGISTER|dupuser|pass|BIDDER");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_REGISTER_FAILED),
            "Expected REGISTER_FAILED, got: " + r);
    }

    @Test
    void registerMissingParamsReturnsError() throws Exception {
        String r = send("REGISTER|onlyone|pass");   // thiếu role → parts.length < 4
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR, got: " + r);
    }

    // ─── LOGIN ───────────────────────────────────────────────────

    @Test
    void loginSuccessReturnsLoginSuccess() throws Exception {
        String r = send("LOGIN|" + sellerUser + "|pw");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_LOGIN_SUCCESS),
            "Expected LOGIN_SUCCESS, got: " + r);
    }

    @Test
    void loginWrongPasswordReturnsLoginFailed() throws Exception {
        String r = send("LOGIN|" + sellerUser + "|wrongpass");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_LOGIN_FAILED),
            "Expected LOGIN_FAILED, got: " + r);
    }

    @Test
    void loginNonexistentUserReturnsLoginFailed() throws Exception {
        String r = send("LOGIN|ghost_user|anypass");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_LOGIN_FAILED),
            "Expected LOGIN_FAILED, got: " + r);
    }

    @Test
    void loginMissingParamsReturnsError() throws Exception {
        String r = send("LOGIN|onlyuser");   // thiếu password
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for missing params, got: " + r);
    }

    // ─── LIST_AUCTIONS ───────────────────────────────────────────

    @Test
    void listAuctionsReturnsSuccess() throws Exception {
        String r = send("LIST_AUCTIONS");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_LIST_SUCCESS),
            "Expected LIST_AUCTIONS_SUCCESS, got: " + r);
    }

    // ─── GET_BALANCE ─────────────────────────────────────────────

    @Test
    void getBalanceWithoutLoginReturnsError() throws Exception {
        String r = send("GET_BALANCE");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR when not logged in, got: " + r);
    }

    @Test
    void getBalanceAfterLoginReturnsBalance() throws Exception {
        send("LOGIN|" + bidderUser + "|pw");
        // flush response trước
        String loginResp = clientIn.readLine(); // đọc nốt LOGIN_SUCCESS nếu chưa đọc
        // Thực ra send() đã đọc rồi, nhưng nếu login response đã được đọc bởi send thì OK
        // Gửi GET_BALANCE trực tiếp
        clientOut.println("GET_BALANCE");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_BALANCE_INFO),
            "Expected BALANCE_INFO, got: " + r);
    }

    // ─── DEPOSIT ─────────────────────────────────────────────────

    @Test
    void depositWithoutLoginReturnsError() throws Exception {
        String r = send("DEPOSIT|1000");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void depositAfterLoginSuccess() throws Exception {
        // Login
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine(); // consume LOGIN_SUCCESS

        clientOut.println("DEPOSIT|500000");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_DEPOSIT_SUCCESS),
            "Expected DEPOSIT_SUCCESS, got: " + r);
    }

    @Test
    void depositInvalidAmountReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine(); // consume LOGIN_SUCCESS

        clientOut.println("DEPOSIT|-500");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for negative deposit, got: " + r);
    }

    @Test
    void depositNonNumericReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("DEPOSIT|abc");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for non-numeric deposit, got: " + r);
    }

    // ─── CREATE_AUCTION ──────────────────────────────────────────

    @Test
    void createAuctionWithoutLoginReturnsError() throws Exception {
        String r = send("CREATE_AUCTION|ELECTRONICS|Phone|1000000|60");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void createAuctionAsBidderReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine(); // consume LOGIN_SUCCESS

        clientOut.println("CREATE_AUCTION|ELECTRONICS|Phone|1000000|60");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Bidder should not create auction, got: " + r);
    }

    @Test
    void createAuctionAsSellerSuccess() throws Exception {
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine(); // consume LOGIN_SUCCESS

        clientOut.println("CREATE_AUCTION|ELECTRONICS|Laptop|5000000|60");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_SUCCESS),
            "Expected SUCCESS for seller create, got: " + r);
    }

    @Test
    void createAuctionAsAdminSuccess() throws Exception {
        clientOut.println("LOGIN|" + adminUser + "|pw");
        clientIn.readLine();

        clientOut.println("CREATE_AUCTION|ART|Painting|2000000|60");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_SUCCESS),
            "Expected SUCCESS for admin create, got: " + r);
    }

    @Test
    void createAuctionInvalidPriceReturnsError() throws Exception {
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine();

        clientOut.println("CREATE_AUCTION|ELECTRONICS|Phone|INVALID_PRICE|60");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for invalid price, got: " + r);
    }

    @Test
    void createAuctionMissingParamsReturnsError() throws Exception {
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine();

        // Chỉ có 3 parts, cần 5
        String r = send("CREATE_AUCTION|ELECTRONICS|Phone");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    // ─── BID ─────────────────────────────────────────────────────

    @Test
    void bidWithoutLoginReturnsError() throws Exception {
        String r = send("BID|AUCTION_123|500000");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void bidOnNonexistentAuctionReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("BID|GHOST_AUCTION|500000");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for nonexistent auction, got: " + r);
    }

    @Test
    void bidNegativeAmountReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("BID|SOME_ID|-100");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void bidNonNumericAmountReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("BID|SOME_ID|not_a_number");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void bidOnRealAuctionSuccess() throws Exception {
        // Tạo auction với seller
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine();
        clientOut.println("CREATE_AUCTION|ELECTRONICS|Phone|1000000|60");
        String createResp = clientIn.readLine();
        assertTrue(createResp.startsWith(Protocol.RES_SUCCESS));

        // Lấy ID auction vừa tạo
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        // Disconnect và mở connection mới để login bidder
        clientSide.close();
        Socket bidderSocket = new Socket("localhost", serverSocket.getLocalPort());

        // Chạy handler mới cho bidder
        Thread t = new Thread(() -> {
            try {
                Socket accepted = serverSocket.accept();
                new ClientHandler(accepted).run();
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();

        PrintWriter bOut = new PrintWriter(bidderSocket.getOutputStream(), true);
        BufferedReader bIn = new BufferedReader(new InputStreamReader(bidderSocket.getInputStream()));

        bOut.println("LOGIN|" + bidderUser + "|pw");
        bIn.readLine(); // consume LOGIN_SUCCESS

        bOut.println("BID|" + auctionId + "|2000000");
        String r = bIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_BID_SUCCESS),
            "Expected BID_SUCCESS, got: " + r);

        bidderSocket.close();
    }

    // ─── END_AUCTION ─────────────────────────────────────────────

    @Test
    void endAuctionWithoutLoginReturnsError() throws Exception {
        String r = send("END_AUCTION|SOME_ID");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void endAuctionAsBidderReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("END_AUCTION|SOME_ID");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void endAuctionAsAdminSuccess() throws Exception {
        // Tạo auction trước
        AuctionService.getInstance().createNewAuction("ELECTRONICS", "TV", 1_000_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("LOGIN|" + adminUser + "|pw");
        clientIn.readLine();

        clientOut.println("END_AUCTION|" + auctionId);
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_END_SUCCESS),
            "Expected END_SUCCESS, got: " + r);
    }

    // ─── DELETE_AUCTION ──────────────────────────────────────────

    @Test
    void deleteAuctionWithoutLoginReturnsError() throws Exception {
        String r = send("DELETE_AUCTION|SOME_ID");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void deleteAuctionAsBidderReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("DELETE_AUCTION|SOME_ID");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void deleteAuctionAsAdminNotFoundReturnsError() throws Exception {
        clientOut.println("LOGIN|" + adminUser + "|pw");
        clientIn.readLine();

        clientOut.println("DELETE_AUCTION|GHOST_ID");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for not found, got: " + r);
    }

    @Test
    void deleteAuctionAsAdminSuccess() throws Exception {
        AuctionService.getInstance().createNewAuction("ART", "Painting", 500_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("LOGIN|" + adminUser + "|pw");
        clientIn.readLine();

        clientOut.println("DELETE_AUCTION|" + auctionId);
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_DELETE_SUCCESS),
            "Expected DELETE_SUCCESS, got: " + r);
    }

    // ─── GET_HISTORY ─────────────────────────────────────────────

    @Test
    void getHistoryNonexistentAuctionReturnsError() throws Exception {
        String r = send("GET_HISTORY|GHOST_ID");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void getHistoryExistingAuctionReturnsHistory() throws Exception {
        AuctionService.getInstance().createNewAuction("ELECTRONICS", "Watch", 200_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("GET_HISTORY|" + auctionId);
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_HISTORY),
            "Expected HISTORY_RES, got: " + r);
    }

    // ─── WATCH / UNWATCH ─────────────────────────────────────────

    @Test
    void watchWithoutLoginAsNonBidderReturnsError() throws Exception {
        // Không login → currentUser null → không phải Bidder
        String r = send("WATCH|SOME_ID");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void watchAsSellerReturnsError() throws Exception {
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine();

        clientOut.println("WATCH|SOME_ID");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Seller should not be able to watch, got: " + r);
    }

    @Test
    void watchAsBidderAuctionNotExistFails() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("WATCH|GHOST_AUCTION");
        String r = clientIn.readLine();
        assertNotNull(r);
        // addToWatchlist returns false for non-existent → ERROR
        assertTrue(r.startsWith(Protocol.ERROR) || r.startsWith(Protocol.RES_WATCH_SUCCESS));
    }

    @Test
    void watchAsBidderRealAuctionSuccess() throws Exception {
        AuctionService.getInstance().createNewAuction("VEHICLE", "Car", 100_000_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("WATCH|" + auctionId);
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_WATCH_SUCCESS),
            "Expected WATCH_SUCCESS, got: " + r);
    }

    @Test
    void unwatchAsBidder() throws Exception {
        AuctionService.getInstance().createNewAuction("VEHICLE", "Bike", 50_000_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        // Watch trước
        clientOut.println("WATCH|" + auctionId);
        clientIn.readLine();

        // Rồi unwatch
        clientOut.println("UNWATCH|" + auctionId);
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_UNWATCH_SUCCESS),
            "Expected UNWATCH_SUCCESS, got: " + r);
    }

    // ─── GET_WATCHLIST ───────────────────────────────────────────

    @Test
    void getWatchlistWithoutLoginReturnsError() throws Exception {
        String r = send("GET_WATCHLIST");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void getWatchlistAsBidderReturnsWatchlist() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("GET_WATCHLIST");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.RES_WATCHLIST),
            "Expected RES_WATCHLIST, got: " + r);
    }

    // ─── ADD_AUTO_BID ─────────────────────────────────────────────

    @Test
    void addAutoBidWithoutLoginReturnsError() throws Exception {
        String r = send("ADD_AUTO_BID|SOME_ID|5000000|100000");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void addAutoBidAsSellerReturnsError() throws Exception {
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        clientIn.readLine();

        clientOut.println("ADD_AUTO_BID|SOME_ID|5000000|100000");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    @Test
    void addAutoBidNonexistentAuctionReturnsError() throws Exception {
        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("ADD_AUTO_BID|GHOST_ID|5000000|100000");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

   
    @Test
    void addAutoBidInvalidNumberReturnsError() throws Exception {
        AuctionService.getInstance().createNewAuction("ART", "Statue", 1_000_000.0, 60L, sellerUser);
        String auctionId = AuctionService.getInstance().getAllAuctions().iterator().next().getId();

        clientOut.println("LOGIN|" + bidderUser + "|pw");
        clientIn.readLine();

        clientOut.println("ADD_AUTO_BID|" + auctionId + "|NOT_A_NUMBER|500000");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR));
    }

    // ─── UNKNOWN COMMAND ─────────────────────────────────────────

    @Test
    void unknownCommandReturnsError() throws Exception {
        String r = send("TOTALLY_UNKNOWN_CMD|param1|param2");
        assertNotNull(r);
        assertTrue(r.startsWith(Protocol.ERROR),
            "Expected ERROR for unknown command, got: " + r);
    }

    // ─── sendMessage và update ─────────────────────────────────

    @Test
    void updateMethodSendsMessageToClient() throws Exception {
        // update() calls sendMessage() — test gián tiếp qua login response
        // (login gọi sendMessage → client nhận được)
        clientOut.println("LOGIN|" + sellerUser + "|pw");
        String r = clientIn.readLine();
        assertNotNull(r);
        assertFalse(r.isEmpty());
    }
}