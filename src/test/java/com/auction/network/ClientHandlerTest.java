package com.auction.network;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.model.Bidder;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
public class ClientHandlerTest {

    private static ServerSocket serverSocket;
    private static int port;

    private static final String SELLER = "ch_seller";
    private static final String BIDDER = "ch_bidder";
    private static final String ADMIN  = "ch_admin";

    @BeforeAll
    static void setUpAll() throws Exception {
        resetSingletons();
        UserManager.getInstance().register(SELLER, "pw", "SELLER");
        UserManager.getInstance().register(BIDDER, "pw", "BIDDER");
        UserManager.getInstance().register(ADMIN,  "pw", "ADMIN");
        ((Bidder) UserManager.getInstance().findUserByUsername(BIDDER))
                .addBalance(999_999_999.0);

        serverSocket = new ServerSocket(0);
        serverSocket.setReuseAddress(true);
        port = serverSocket.getLocalPort();

        Thread acceptor = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket s = serverSocket.accept();
                    new Thread(new ClientHandler(s)).start();
                } catch (Exception ignored) {}
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        try { serverSocket.close(); } catch (Exception ignored) {}
        resetSingletons();
    }

    private static void resetSingletons() throws Exception {
        Field asf = AuctionService.class.getDeclaredField("instance");
        asf.setAccessible(true);
        asf.set(null, null);
        Field umf = UserManager.class.getDeclaredField("instance");
        umf.setAccessible(true);
        umf.set(null, null);
    }

    private String[] chat(String... cmds) throws Exception {
        try (Socket s = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String[] results = new String[cmds.length];
            for (int i = 0; i < cmds.length; i++) {
                out.println(cmds[i]);
                results[i] = in.readLine();
            }
            return results;
        }
    }

    private String send(String cmd) throws Exception {
        return chat(cmd)[0];
    }

    // ── REGISTER ───────────────────────────────────────────────

    @Test void registerSuccess() throws Exception {
        assertTrue(send("REGISTER|newreg1|pw|BIDDER").startsWith(Protocol.RES_REGISTER_SUCCESS));
    }

    @Test void registerDuplicate() throws Exception {
        send("REGISTER|dupreg|pw|BIDDER");
        assertTrue(send("REGISTER|dupreg|pw|BIDDER").startsWith(Protocol.RES_REGISTER_FAILED));
    }

    @Test void registerMissingParams() throws Exception {
        assertTrue(send("REGISTER|onlyone|pw").startsWith(Protocol.ERROR));
    }

    // ── LOGIN ──────────────────────────────────────────────────

    @Test void loginSuccess() throws Exception {
        assertTrue(send("LOGIN|" + SELLER + "|pw").startsWith(Protocol.RES_LOGIN_SUCCESS));
    }

    @Test void loginWrongPassword() throws Exception {
        assertTrue(send("LOGIN|" + SELLER + "|wrong").startsWith(Protocol.RES_LOGIN_FAILED));
    }

    @Test void loginUnknownUser() throws Exception {
        assertTrue(send("LOGIN|ghost_xyz|pw").startsWith(Protocol.RES_LOGIN_FAILED));
    }

    @Test void loginMissingParams() throws Exception {
        assertTrue(send("LOGIN|onlyuser").startsWith(Protocol.ERROR));
    }

    // ── LIST_AUCTIONS ──────────────────────────────────────────

    @Test void listAuctions() throws Exception {
        assertTrue(send("LIST_AUCTIONS").startsWith(Protocol.RES_LIST_SUCCESS));
    }

    // ── GET_BALANCE ────────────────────────────────────────────

    @Test void getBalanceNotLoggedIn() throws Exception {
        assertTrue(send("GET_BALANCE").startsWith(Protocol.ERROR));
    }

    @Test void getBalanceAfterLogin() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "GET_BALANCE");
        assertTrue(r[1].startsWith(Protocol.RES_BALANCE_INFO));
    }

    // ── DEPOSIT ────────────────────────────────────────────────

    @Test void depositNotLoggedIn() throws Exception {
        assertTrue(send("DEPOSIT|1000").startsWith(Protocol.ERROR));
    }

    @Test void depositSuccess() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "DEPOSIT|500000");
        assertTrue(r[1].startsWith(Protocol.RES_DEPOSIT_SUCCESS));
    }

    @Test void depositNegative() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "DEPOSIT|-100");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void depositNonNumeric() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "DEPOSIT|abc");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    // ── CREATE_AUCTION ─────────────────────────────────────────

    @Test void createNotLoggedIn() throws Exception {
        assertTrue(send("CREATE_AUCTION|ELECTRONICS|Phone|1000000|60").startsWith(Protocol.ERROR));
    }

    @Test void createAsBidderForbidden() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "CREATE_AUCTION|ELECTRONICS|Phone|1000000|60");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void createAsSellerSuccess() throws Exception {
        String[] r = chat("LOGIN|" + SELLER + "|pw", "CREATE_AUCTION|ELECTRONICS|Laptop|5000000|60");
        assertTrue(r[1].startsWith(Protocol.RES_SUCCESS));
    }

    @Test void createAsAdminSuccess() throws Exception {
        String[] r = chat("LOGIN|" + ADMIN + "|pw", "CREATE_AUCTION|ART|Painting|2000000|60");
        assertTrue(r[1].startsWith(Protocol.RES_SUCCESS));
    }

    @Test void createInvalidPrice() throws Exception {
        String[] r = chat("LOGIN|" + SELLER + "|pw", "CREATE_AUCTION|ELECTRONICS|Phone|BAD|60");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void createMissingParams() throws Exception {
        String[] r = chat("LOGIN|" + SELLER + "|pw", "CREATE_AUCTION|ELECTRONICS|Phone");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    // ── BID ────────────────────────────────────────────────────

    @Test void bidNotLoggedIn() throws Exception {
        assertTrue(send("BID|GHOST|500000").startsWith(Protocol.ERROR));
    }

    @Test void bidGhostAuction() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "BID|GHOST_ID|500000");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void bidNegativeAmount() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "BID|SOME|-100");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void bidNonNumeric() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "BID|SOME|abc");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

   

    // ── END_AUCTION ────────────────────────────────────────────

    @Test void endNotLoggedIn() throws Exception {
        assertTrue(send("END_AUCTION|X").startsWith(Protocol.ERROR));
    }

    @Test void endAsBidderForbidden() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "END_AUCTION|X");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void endAsAdminSuccess() throws Exception {
        AuctionService.getInstance()
                .createNewAuction("ELECTRONICS", "TVEnd", 1_000_000.0, 60L, SELLER);
        String id = AuctionService.getInstance().getAllAuctions().stream()
                .filter(a -> a.getItem() != null && "TVEnd".equals(a.getItem().getItemName()))
                .findFirst().get().getId();
        String[] r = chat("LOGIN|" + ADMIN + "|pw", "END_AUCTION|" + id);
        assertTrue(r[1].startsWith(Protocol.RES_END_SUCCESS));
    }

    // ── DELETE_AUCTION ─────────────────────────────────────────

    @Test void deleteNotLoggedIn() throws Exception {
        assertTrue(send("DELETE_AUCTION|X").startsWith(Protocol.ERROR));
    }

    @Test void deleteAsBidderForbidden() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "DELETE_AUCTION|X");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void deleteGhostAuction() throws Exception {
        String[] r = chat("LOGIN|" + ADMIN + "|pw", "DELETE_AUCTION|GHOST");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void deleteSuccess() throws Exception {
        AuctionService.getInstance()
                .createNewAuction("ART", "StatueDel", 500_000.0, 60L, SELLER);
        String id = AuctionService.getInstance().getAllAuctions().stream()
                .filter(a -> a.getItem() != null && "StatueDel".equals(a.getItem().getItemName()))
                .findFirst().get().getId();
        String[] r = chat("LOGIN|" + ADMIN + "|pw", "DELETE_AUCTION|" + id);
        assertTrue(r[1].startsWith(Protocol.RES_DELETE_SUCCESS));
    }

    // ── GET_HISTORY ────────────────────────────────────────────

    @Test void historyGhostAuction() throws Exception {
        assertTrue(send("GET_HISTORY|GHOST").startsWith(Protocol.ERROR));
    }

    @Test void historyRealAuction() throws Exception {
        AuctionService.getInstance()
                .createNewAuction("VEHICLE", "CarHist", 10_000_000.0, 60L, SELLER);
        String id = AuctionService.getInstance().getAllAuctions().stream()
                .filter(a -> a.getItem() != null && "CarHist".equals(a.getItem().getItemName()))
                .findFirst().get().getId();
        assertTrue(send("GET_HISTORY|" + id).startsWith(Protocol.RES_HISTORY));
    }

    // ── WATCH / UNWATCH / GET_WATCHLIST ───────────────────────

    @Test void watchNotLoggedIn() throws Exception {
        assertTrue(send("WATCH|X").startsWith(Protocol.ERROR));
    }

    @Test void watchAsSeller() throws Exception {
        String[] r = chat("LOGIN|" + SELLER + "|pw", "WATCH|X");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void watchAndUnwatchSuccess() throws Exception {
        AuctionService.getInstance()
                .createNewAuction("VEHICLE", "BikeWatch", 5_000_000.0, 60L, SELLER);
        String id = AuctionService.getInstance().getAllAuctions().stream()
                .filter(a -> a.getItem() != null && "BikeWatch".equals(a.getItem().getItemName()))
                .findFirst().get().getId();
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "WATCH|" + id, "UNWATCH|" + id);
        assertTrue(r[1].startsWith(Protocol.RES_WATCH_SUCCESS));
        assertTrue(r[2].startsWith(Protocol.RES_UNWATCH_SUCCESS));
    }

    @Test void getWatchlistNotLoggedIn() throws Exception {
        assertTrue(send("GET_WATCHLIST").startsWith(Protocol.ERROR));
    }

    @Test void getWatchlistAsBidder() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "GET_WATCHLIST");
        assertTrue(r[1].startsWith(Protocol.RES_WATCHLIST));
    }

    // ── ADD_AUTO_BID ───────────────────────────────────────────

    @Test void autoBidNotLoggedIn() throws Exception {
        assertTrue(send("ADD_AUTO_BID|X|5000000|100000").startsWith(Protocol.ERROR));
    }

    @Test void autoBidAsSeller() throws Exception {
        String[] r = chat("LOGIN|" + SELLER + "|pw", "ADD_AUTO_BID|X|5000000|100000");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    @Test void autoBidGhostAuction() throws Exception {
        String[] r = chat("LOGIN|" + BIDDER + "|pw", "ADD_AUTO_BID|GHOST|5000000|100000");
        assertTrue(r[1].startsWith(Protocol.ERROR));
    }

    

    // ── UNKNOWN COMMAND ────────────────────────────────────────

    @Test void unknownCommand() throws Exception {
        assertTrue(send("UNKNOWN_CMD|x|y").startsWith(Protocol.ERROR));
    }
}