package com.auction.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.controller.command.AddAutoBidCommand;
import com.auction.controller.command.BidCommand;
import com.auction.controller.command.CreateAuctionCommand;
import com.auction.controller.command.DepositCommand;
import com.auction.controller.command.EndAuctionCommand;
import com.auction.controller.command.GetBalanceCommand;
import com.auction.controller.command.GetHistoryCommand;
import com.auction.controller.command.GetWatchlistCommand;
import com.auction.controller.command.ListAuctionsCommand;
import com.auction.controller.command.LoginCommand;
import com.auction.controller.command.RegisterCommand;
import com.auction.controller.command.UnwatchCommand;
import com.auction.controller.command.WatchCommand;
import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Admin;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.Seller;
import com.auction.model.entities.user.User;
import com.auction.model.enums.AuctionStatus;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for all controller command classes.
 * Uses ClientHandler(null) – sendMessage does nothing since 'out' is null.
 */
public class CommandTest {

  private ClientHandler handler;
  private AuctionService auctionService;

  @BeforeEach
  void setUp() throws Exception {
    resetSingletons();
    auctionService = AuctionService.getInstance();
    // ClientHandler with null socket: sendMessage() is silently no-op (out == null)
    handler = new ClientHandler(null);
  }

  @AfterEach
  void tearDown() {
    try {
      auctionService.shutdown();
    } catch (Exception ignored) {
    }
    cleanFiles();
  }

  // ===========================
  // RegisterCommand
  // ===========================

  @Test
  void registerCommandSuccessAddsUser() {
    String[] parts = { "REGISTER", "alice", "pw123", "BIDDER", "alice@test.com" };
    new RegisterCommand().execute(parts, handler, auctionService);
    assertNotNull(UserManager.getInstance().findUserByUsername("alice"));
  }

  @Test
  void registerCommandDuplicateUsernameDoesNotThrow() {
    String[] parts = { "REGISTER", "bob", "pw", "BIDDER", "bob@test.com" };
    new RegisterCommand().execute(parts, handler, auctionService);
    // Second call with same username should not throw
    assertDoesNotThrow(() -> new RegisterCommand().execute(parts, handler, auctionService));
  }

  @Test
  void registerCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "REGISTER", "bob" };
    assertDoesNotThrow(() -> new RegisterCommand().execute(parts, handler, auctionService));
  }

  @Test
  void registerCommandCreatesSellerInstance() {
    String[] parts = { "REGISTER", "seller1", "pw", "SELLER", "seller1@test.com" };
    new RegisterCommand().execute(parts, handler, auctionService);
    assertTrue(UserManager.getInstance().findUserByUsername("seller1") instanceof Seller);
  }

  @Test
  void registerCommandCreatesAdminInstance() {
    String[] parts = { "REGISTER", "admin2", "pw", "ADMIN", "admin2@test.com" };
    new RegisterCommand().execute(parts, handler, auctionService);
    assertTrue(UserManager.getInstance().findUserByUsername("admin2") instanceof Admin);
  }

  // ===========================
  // LoginCommand
  // ===========================

  @Test
  void loginCommandSuccessSetsCurrentUser() {
    UserManager.getInstance().register("carol", "secret", "BIDDER", "carol@test.com");
    String[] parts = { "LOGIN", "carol", "secret" };
    new LoginCommand().execute(parts, handler, auctionService);
    assertNotNull(handler.getCurrentUser());
    assertEquals("carol", handler.getCurrentUser().getUsername());
  }

  @Test
  void loginCommandWrongPasswordDoesNotSetUser() {
    UserManager.getInstance().register("dave", "correct", "BIDDER", "dave@test.com");
    String[] parts = { "LOGIN", "dave", "wrong" };
    new LoginCommand().execute(parts, handler, auctionService);
    assertNull(handler.getCurrentUser());
  }

  @Test
  void loginCommandNonExistentUserDoesNotSetUser() {
    String[] parts = { "LOGIN", "ghost", "pw" };
    new LoginCommand().execute(parts, handler, auctionService);
    assertNull(handler.getCurrentUser());
  }

  @Test
  void loginCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "LOGIN" };
    assertDoesNotThrow(() -> new LoginCommand().execute(parts, handler, auctionService));
  }

  @Test
  void loginCommandSuccessRolePreserved() {
    UserManager.getInstance().register("eva", "pw", "SELLER", "eva@test.com");
    String[] parts = { "LOGIN", "eva", "pw" };
    new LoginCommand().execute(parts, handler, auctionService);
    assertEquals("SELLER", handler.getCurrentUser().getRole());
  }

  // ===========================
  // GetBalanceCommand
  // ===========================

  @Test
  void getBalanceCommandNotLoggedInDoesNotThrow() {
    assertDoesNotThrow(() -> new GetBalanceCommand().execute(new String[] { "GET_BALANCE" }, handler, auctionService));
  }

  @Test
  void getBalanceCommandLoggedInDoesNotThrow() {
    UserManager.getInstance().register("frank", "pw", "BIDDER", "frank@test.com");
    Bidder frank = (Bidder) UserManager.getInstance().findUserByUsername("frank");
    frank.addBalance(1_000_000.0);
    handler.setCurrentUser(frank);
    assertDoesNotThrow(() -> new GetBalanceCommand().execute(new String[] { "GET_BALANCE" }, handler, auctionService));
  }

  // ===========================
  // DepositCommand
  // ===========================

  @Test
  void depositCommandNotLoggedInDoesNotThrow() {
    String[] parts = { "DEPOSIT", "500000" };
    assertDoesNotThrow(() -> new DepositCommand().execute(parts, handler, auctionService));
  }

  @Test
  void depositCommandValidAmountIncreasesBalance() {
    UserManager.getInstance().register("grace", "pw", "BIDDER", "grace@test.com");
    Bidder grace = (Bidder) UserManager.getInstance().findUserByUsername("grace");
    handler.setCurrentUser(grace);
    double before = grace.getBalance();

    String[] parts = { "DEPOSIT", "1000000" };
    new DepositCommand().execute(parts, handler, auctionService);

    assertEquals(before + 1_000_000.0, grace.getBalance(), 0.001);
  }

  @Test
  void depositCommandNegativeAmountDoesNotChangeBalance() {
    UserManager.getInstance().register("henry", "pw", "BIDDER", "henry@test.com");
    Bidder henry = (Bidder) UserManager.getInstance().findUserByUsername("henry");
    henry.addBalance(500_000.0);
    handler.setCurrentUser(henry);

    String[] parts = { "DEPOSIT", "-100000" };
    new DepositCommand().execute(parts, handler, auctionService);

    assertEquals(500_000.0, henry.getBalance(), 0.001);
  }

  @Test
  void depositCommandInvalidNumberDoesNotThrow() {
    UserManager.getInstance().register("iris", "pw", "BIDDER", "iris@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("iris"));
    String[] parts = { "DEPOSIT", "notANumber" };
    assertDoesNotThrow(() -> new DepositCommand().execute(parts, handler, auctionService));
  }

  @Test
  void depositCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "DEPOSIT" };
    assertDoesNotThrow(() -> new DepositCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // CreateAuctionCommand
  // ===========================

  @Test
  void createAuctionCommandNotLoggedInDoesNotThrow() {
    String[] parts = { "CREATE_AUCTION", "ELECTRONICS", "TV", "500000", "60" };
    assertDoesNotThrow(() -> new CreateAuctionCommand().execute(parts, handler, auctionService));
  }

  @Test
  void createAuctionCommandBidderRoleIsRejected() {
    UserManager.getInstance().register("jack", "pw", "BIDDER", "jack@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("jack"));
    String[] parts = { "CREATE_AUCTION", "ELECTRONICS", "Phone", "500000", "60" };
    new CreateAuctionCommand().execute(parts, handler, auctionService);
    assertTrue(auctionService.getAllAuctions().isEmpty());
  }

  @Test
  void createAuctionCommandInvalidPriceDoesNotThrow() {
    UserManager.getInstance().register("leo", "pw", "SELLER", "leo@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("leo"));
    String[] parts = { "CREATE_AUCTION", "ELECTRONICS", "TV", "notANumber", "60" };
    assertDoesNotThrow(() -> new CreateAuctionCommand().execute(parts, handler, auctionService));
  }

  @Test
  void createAuctionCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "CREATE_AUCTION", "ELECTRONICS" };
    assertDoesNotThrow(() -> new CreateAuctionCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // ListAuctionsCommand
  // ===========================

  @Test
  void listAuctionsCommandEmptyServiceDoesNotThrow() {
    assertDoesNotThrow(
        () -> new ListAuctionsCommand().execute(new String[] { "LIST_AUCTIONS" }, handler, auctionService));
  }

  @Test
  void listAuctionsCommandWithAuctionsDoesNotThrow() {
    UserManager.getInstance().register("list_seller", "pw", "SELLER", "list_seller@test.com");
    auctionService.createNewAuction("ELECTRONICS", "Headset", 500_000.0, 9999L, "list_seller", "", "");
    assertDoesNotThrow(
        () -> new ListAuctionsCommand().execute(new String[] { "LIST_AUCTIONS" }, handler, auctionService));
  }

  // ===========================
  // GetHistoryCommand
  // ===========================

  @Test
  void getHistoryCommandAuctionNotFoundDoesNotThrow() {
    String[] parts = { "GET_HISTORY", "GHOST_AUCTION" };
    assertDoesNotThrow(() -> new GetHistoryCommand().execute(parts, handler, auctionService));
  }

  @Test
  void getHistoryCommandValidAuctionDoesNotThrow() {
    UserManager.getInstance().register("hist_seller", "pw", "SELLER", "hist_seller@test.com");
    auctionService.createNewAuction("ELECTRONICS", "HistItem", 500_000.0, 9999L, "hist_seller", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();
    String[] parts = { "GET_HISTORY", a.getId() };
    assertDoesNotThrow(() -> new GetHistoryCommand().execute(parts, handler, auctionService));
  }

  @Test
  void getHistoryCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "GET_HISTORY" };
    assertDoesNotThrow(() -> new GetHistoryCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // WatchCommand
  // ===========================

  @Test
  void watchCommandNotBidderDoesNotThrow() {
    UserManager.getInstance().register("watch_seller", "pw", "SELLER", "watch_seller@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("watch_seller"));
    String[] parts = { "WATCH", "AUC_123" };
    assertDoesNotThrow(() -> new WatchCommand().execute(parts, handler, auctionService));
  }

  @Test
  void watchCommandBidderAddsToWatchlist() {
    UserManager.getInstance().register("watcher3", "pw", "BIDDER", "watcher3@test.com");
    Bidder watcher = (Bidder) UserManager.getInstance().findUserByUsername("watcher3");
    handler.setCurrentUser(watcher);

    UserManager.getInstance().register("ws2", "pw", "SELLER", "ws2@test.com");
    auctionService.createNewAuction("ELECTRONICS", "WatchItem", 500_000.0, 9999L, "ws2", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();

    String[] parts = { "WATCH", a.getId() };
    new WatchCommand().execute(parts, handler, auctionService);

    assertTrue(watcher.getWatchlist().contains(a.getId()));
  }

  @Test
  void watchCommandDuplicateDoesNotThrow() {
    UserManager.getInstance().register("watcher4", "pw", "BIDDER", "watcher4@test.com");
    Bidder watcher = (Bidder) UserManager.getInstance().findUserByUsername("watcher4");
    handler.setCurrentUser(watcher);

    String[] parts = { "WATCH", "AUC_DUP" };
    new WatchCommand().execute(parts, handler, auctionService);
    // Second call should not throw
    assertDoesNotThrow(() -> new WatchCommand().execute(parts, handler, auctionService));
  }

  @Test
  void watchCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "WATCH" };
    assertDoesNotThrow(() -> new WatchCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // UnwatchCommand
  // ===========================

  @Test
  void unwatchCommandNotBidderDoesNotThrow() {
    UserManager.getInstance().register("unseller", "pw", "SELLER", "unseller@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("unseller"));
    String[] parts = { "UNWATCH", "AUC_123" };
    assertDoesNotThrow(() -> new UnwatchCommand().execute(parts, handler, auctionService));
  }

  @Test
  void unwatchCommandBidderRemovesFromWatchlist() {
    UserManager.getInstance().register("unwatcher", "pw", "BIDDER", "unwatcher@test.com");
    Bidder unwatcher = (Bidder) UserManager.getInstance().findUserByUsername("unwatcher");
    unwatcher.addToWatchlist("AUC_UNWATCH");
    handler.setCurrentUser(unwatcher);

    String[] parts = { "UNWATCH", "AUC_UNWATCH" };
    new UnwatchCommand().execute(parts, handler, auctionService);

    assertFalse(unwatcher.getWatchlist().contains("AUC_UNWATCH"));
  }

  @Test
  void unwatchCommandWithExistingAuctionDoesNotThrow() {
    UserManager.getInstance().register("unw2", "pw", "BIDDER", "unw2@test.com");
    Bidder unw2 = (Bidder) UserManager.getInstance().findUserByUsername("unw2");
    handler.setCurrentUser(unw2);

    UserManager.getInstance().register("uns2", "pw", "SELLER", "uns2@test.com");
    auctionService.createNewAuction("ELECTRONICS", "UnwatchItem", 500_000.0, 9999L, "uns2", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();
    unw2.addToWatchlist(a.getId());

    String[] parts = { "UNWATCH", a.getId() };
    assertDoesNotThrow(() -> new UnwatchCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // GetWatchlistCommand
  // ===========================

  @Test
  void getWatchlistCommandNotBidderDoesNotThrow() {
    UserManager.getInstance().register("gwl_seller", "pw", "SELLER", "gwl_seller@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("gwl_seller"));
    assertDoesNotThrow(
        () -> new GetWatchlistCommand().execute(new String[] { "GET_WATCHLIST" }, handler, auctionService));
  }

  @Test
  void getWatchlistCommandBidderDoesNotThrow() {
    UserManager.getInstance().register("gwl_bidder", "pw", "BIDDER", "gwl_bidder@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("gwl_bidder"));
    assertDoesNotThrow(
        () -> new GetWatchlistCommand().execute(new String[] { "GET_WATCHLIST" }, handler, auctionService));
  }

  @Test
  void getWatchlistCommandNullUserDoesNotThrow() {
    // handler.getCurrentUser() is null by default
    assertDoesNotThrow(
        () -> new GetWatchlistCommand().execute(new String[] { "GET_WATCHLIST" }, handler, auctionService));
  }

  // ===========================
  // EndAuctionCommand
  // ===========================

  @Test
  void endAuctionCommandNotAdminDoesNotThrow() {
    UserManager.getInstance().register("eac_bidder", "pw", "BIDDER", "eac_bidder@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("eac_bidder"));
    String[] parts = { "END_AUCTION", "AUC_123" };
    assertDoesNotThrow(() -> new EndAuctionCommand().execute(parts, handler, auctionService));
  }

  @Test
  void endAuctionCommandNullUserDoesNotThrow() {
    String[] parts = { "END_AUCTION", "AUC_123" };
    assertDoesNotThrow(() -> new EndAuctionCommand().execute(parts, handler, auctionService));
  }

  @Test
  void endAuctionCommandAdminEndsAuction() throws Exception {
    UserManager.getInstance().register("eac_admin", "pw", "ADMIN", "eac_admin@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("eac_admin"));

    UserManager.getInstance().register("eac_seller", "pw", "SELLER", "eac_seller@test.com");
    auctionService.createNewAuction("ELECTRONICS", "EAC_Item", 500_000.0, 9999L, "eac_seller", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();

    // Set endTime to past so endAuction actually proceeds
    Field f = Auction.class.getDeclaredField("endTime");
    f.setAccessible(true);
    f.set(a, System.currentTimeMillis() - 1000L);

    String[] parts = { "END_AUCTION", a.getId() };
    new EndAuctionCommand().execute(parts, handler, auctionService);

    assertEquals(AuctionStatus.FINISHED, a.getStatus());
  }

  @Test
  void endAuctionCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "END_AUCTION" };
    assertDoesNotThrow(() -> new EndAuctionCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // BidCommand
  // ===========================

  @Test
  void bidCommandNotLoggedInDoesNotThrow() {
    String[] parts = { "BID", "AUC_123", "600000" };
    assertDoesNotThrow(() -> new BidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void bidCommandAuctionNotFoundDoesNotThrow() {
    UserManager.getInstance().register("bid_user", "pw", "BIDDER", "bid_user@test.com");
    Bidder bidUser = (Bidder) UserManager.getInstance().findUserByUsername("bid_user");
    bidUser.addBalance(10_000_000.0);
    handler.setCurrentUser(bidUser);

    String[] parts = { "BID", "GHOST_AUCTION", "600000" };
    assertDoesNotThrow(() -> new BidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void bidCommandValidBidAddsToBidHistory() {
    UserManager.getInstance().register("bid_seller", "pw", "SELLER", "bid_seller@test.com");
    UserManager.getInstance().register("bid_bidder", "pw", "BIDDER", "bid_bidder@test.com");
    Bidder bidder = (Bidder) UserManager.getInstance().findUserByUsername("bid_bidder");
    bidder.addBalance(10_000_000.0);
    handler.setCurrentUser(bidder);

    auctionService.createNewAuction("ELECTRONICS", "BidItem", 500_000.0, 9999L, "bid_seller", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();

    String[] parts = { "BID", a.getId(), "550000" };
    new BidCommand().execute(parts, handler, auctionService);

    assertFalse(a.getBidHistory().isEmpty());
  }

  @Test
  void bidCommandInvalidAmountFormatDoesNotThrow() {
    UserManager.getInstance().register("bid_user2", "pw", "BIDDER", "bid_user2@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("bid_user2"));
    String[] parts = { "BID", "AUC_123", "notANumber" };
    assertDoesNotThrow(() -> new BidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void bidCommandNegativeAmountDoesNotThrow() {
    UserManager.getInstance().register("bid_user3", "pw", "BIDDER", "bid_user3@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("bid_user3"));
    String[] parts = { "BID", "AUC_123", "-500" };
    assertDoesNotThrow(() -> new BidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void bidCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "BID" };
    assertDoesNotThrow(() -> new BidCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // AddAutoBidCommand
  // ===========================

  @Test
  void addAutoBidCommandNotBidderDoesNotThrow() {
    UserManager.getInstance().register("aab_seller", "pw", "SELLER", "aab_seller@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("aab_seller"));
    String[] parts = { "ADD_AUTO_BID", "AUC_123", "2000000", "50000" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void addAutoBidCommandNullUserDoesNotThrow() {
    String[] parts = { "ADD_AUTO_BID", "AUC_123", "2000000", "50000" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void addAutoBidCommandAuctionNotFoundDoesNotThrow() {
    UserManager.getInstance().register("aab_bidder", "pw", "BIDDER", "aab_bidder@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("aab_bidder"));
    String[] parts = { "ADD_AUTO_BID", "GHOST_AUC", "2000000", "50000" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void addAutoBidCommandValidConfigDoesNotThrow() {
    UserManager.getInstance().register("aab_seller2", "pw", "SELLER", "aab_seller2@test.com");
    UserManager.getInstance().register("aab_bidder2", "pw", "BIDDER", "aab_bidder2@test.com");
    Bidder bidder = (Bidder) UserManager.getInstance().findUserByUsername("aab_bidder2");
    bidder.addBalance(10_000_000.0);
    handler.setCurrentUser(bidder);

    auctionService.createNewAuction("ELECTRONICS", "AutoItem", 500_000.0, 9999L, "aab_seller2", "", "");
    Auction a = auctionService.getAllAuctions().iterator().next();

    // minIncrement for 500_000 is 50_000, so step=50_000 is valid
    String[] parts = { "ADD_AUTO_BID", a.getId(), "2000000", "50000" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void addAutoBidCommandInvalidNumberDoesNotThrow() {
    UserManager.getInstance().register("aab_bidder3", "pw", "BIDDER", "aab_bidder3@test.com");
    handler.setCurrentUser(UserManager.getInstance().findUserByUsername("aab_bidder3"));
    String[] parts = { "ADD_AUTO_BID", "AUC_123", "notANumber", "50000" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  @Test
  void addAutoBidCommandInsufficientPartsDoesNotThrow() {
    String[] parts = { "ADD_AUTO_BID", "AUC_123" };
    assertDoesNotThrow(() -> new AddAutoBidCommand().execute(parts, handler, auctionService));
  }

  // ===========================
  // ClientHandler utility methods
  // ===========================

  @Test
  void clientHandlerValidatePayloadReturnsTrueWhenSufficient() {
    String[] parts = { "CMD", "arg1", "arg2" };
    assertTrue(handler.validatePayload(parts, 3));
  }

  @Test
  void clientHandlerValidatePayloadReturnsFalseWhenInsufficient() {
    String[] parts = { "CMD" };
    assertFalse(handler.validatePayload(parts, 3));
  }

  @Test
  void clientHandlerValidatePayloadNullReturnsFalse() {
    assertFalse(handler.validatePayload(null, 1));
  }

  @Test
  void clientHandlerSetAndGetCurrentUser() {
    UserManager.getInstance().register("ch_user", "pw", "BIDDER", "ch_user@test.com");
    User user = UserManager.getInstance().findUserByUsername("ch_user");
    handler.setCurrentUser(user);
    assertEquals("ch_user", handler.getCurrentUser().getUsername());
  }

  @Test
  void clientHandlerGetAssociatedUsernameNullWhenNoUser() {
    assertNull(handler.getAssociatedUsername());
  }

  @Test
  void clientHandlerGetAssociatedUsernameReturnsUsername() {
    UserManager.getInstance().register("ch_user2", "pw", "BIDDER", "ch_user2@test.com");
    User user = UserManager.getInstance().findUserByUsername("ch_user2");
    handler.setCurrentUser(user);
    assertEquals("ch_user2", handler.getAssociatedUsername());
  }

  @Test
  void clientHandlerSendMessageDoesNotThrow() {
    // sendMessage with null socket is safe (out == null, short-circuits)
    assertDoesNotThrow(() -> handler.sendMessage("test message"));
  }

  @Test
  void clientHandlerUpdateDoesNotThrow() {
    assertDoesNotThrow(() -> handler.update("update message"));
  }

  @Test
  void clientHandlerGsonIsNotNull() {
    assertNotNull(handler.gson);
  }

  // ===========================
  // helpers
  // ===========================

  private void resetSingletons() throws Exception {
    // DataManager đã chuyển sang Holder idiom (package datamanager).
    // Không có field 'instance' để reset; cũng không giữ business state cần reset.

    // AuctionService vẫn dùng double-checked locking → có field 'instance'.
    Field as = AuctionService.class.getDeclaredField("instance");
    as.setAccessible(true);
    as.set(null, null);

    // UserManager dùng Holder idiom → clear users qua API public.
    UserManager.getInstance().setUsers(new HashMap<>());
  }

  private void cleanFiles() {
    new File("auctions.dat").delete();
    new File("users.dat").delete();
    new File("auctions.dat.tmp").delete();
    new File("users.dat.tmp").delete();
  }
}