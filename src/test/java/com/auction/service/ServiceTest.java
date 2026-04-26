package com.auction.service;
 
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import com.auction.model.Admin;
import com.auction.model.Art;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.model.Vehicle;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
public class ServiceTest {
 
  private static final long DURATION = 9999L;
  private static final double PRICE_LOW = 450.0;
  private static final double PRICE_HIGH = 600.0;
 
  private AuctionService auctionService;
  private UserManager userManager;
 
  @BeforeEach
  void setUp() throws Exception {
    Field aField = AuctionService.class.getDeclaredField("instance");
    aField.setAccessible(true);
    aField.set(null, null);
    auctionService = AuctionService.getInstance();
 
    Field uField = UserManager.class.getDeclaredField("instance");
    uField.setAccessible(true);
    uField.set(null, null);
    userManager = UserManager.getInstance();
  }
 
  // ================================================================
  // AuctionService — Singleton
  // ================================================================
 
  @Test
  void auctionServiceGetInstanceReturnsSameObject() {
    assertSame(AuctionService.getInstance(), AuctionService.getInstance());
  }
 
  // ================================================================
  // AuctionService — addAuction / getAuctionById
  // ================================================================
 
  @Test
  void addAuctionThenGetByIdReturnsCorrectAuction() {
    Item item = new Electronics("e1", "Phone", 500.0);
    auctionService.addAuction(new Auction("a1", item, DURATION));
    assertNotNull(auctionService.getAuctionById("a1"));
  }
 
  @Test
  void addAuctionThenGetByIdHasCorrectId() {
    Item item = new Electronics("e2", "TV", 800.0);
    auctionService.addAuction(new Auction("a2", item, DURATION));
    assertEquals("a2", auctionService.getAuctionById("a2").getId());
  }
 
  @Test
  void addAuctionNullInputDoesNotThrow() {
    assertDoesNotThrow(() -> auctionService.addAuction(null));
  }
 
  @Test
  void getAuctionByIdNonExistentReturnsNull() {
    assertNull(auctionService.getAuctionById("ghost-id"));
  }
 
  // ================================================================
  // AuctionService — getAllAuctions
  // ================================================================
 
  @Test
  void getAllAuctionsAfterAddingTwoReturnsSizeAtLeastTwo() {
    auctionService.addAuction(new Auction("a3", new Art("art1", "Painting", 200.0), DURATION));
    auctionService.addAuction(new Auction("a4", new Vehicle("v1", "Honda", 5000.0), DURATION));
    assertTrue(auctionService.getAllAuctions().size() >= 2);
  }
 
  // ================================================================
  // AuctionService — endAuction
  // ================================================================
 
  @Test
  void endAuctionRunningStatusSetsFinished() {
    Auction auction = new Auction("a-end-1", new Electronics("e3", "Tablet", 300.0), DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    auctionService.addAuction(auction);
    auctionService.endAuction("a-end-1");
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void endAuctionOpenStatusSetsFinished() {
    Auction auction = new Auction("a-end-2", new Electronics("e4", "Camera", 400.0), DURATION);
    auctionService.addAuction(auction);
    auctionService.endAuction("a-end-2");
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());
  }
 
  @Test
  void endAuctionAlreadyFinishedDoesNotThrow() {
    Auction auction = new Auction("a-end-3", new Art("art2", "Vase", 300.0), DURATION);
    auction.setStatus(AuctionStatus.FINISHED);
    auctionService.addAuction(auction);
    assertDoesNotThrow(() -> auctionService.endAuction("a-end-3"));
  }
 
  @Test
  void endAuctionNonExistentIdDoesNotThrow() {
    assertDoesNotThrow(() -> auctionService.endAuction("non-existent"));
  }
 
  @Test
  void endAuctionWithBidsNotifiesWinnerUsername() throws Exception {
    Auction auction = new Auction("a-end-4", new Electronics("e5", "Laptop", 400.0), DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    auctionService.addAuction(auction);
    auction.processNewBid(new Bidder("charlie", "pw"), PRICE_LOW);
    auction.processNewBid(new Bidder("diana", "pw"), PRICE_HIGH);
 
    final String[] lastMsg = {""};
    auction.addObserver(msg -> lastMsg[0] = msg);
 
    auctionService.endAuction("a-end-4");
    assertTrue(lastMsg[0].contains("diana"));
  }
 
  @Test
  void endAuctionNoBidsNotifiesNoWinner() {
    Auction auction = new Auction("a-end-5", new Art("art3", "Sculpture", 1000.0), DURATION);
    auction.setStatus(AuctionStatus.RUNNING);
    auctionService.addAuction(auction);
 
    final String[] lastMsg = {""};
    auction.addObserver(msg -> lastMsg[0] = msg);
 
    auctionService.endAuction("a-end-5");
    assertTrue(lastMsg[0].contains("No winner"));
  }
 
  // ================================================================
  // AuctionService — getItemInAuction / getAuctionsMap
  // ================================================================
 
  @Test
  void getItemInAuctionExistingIdReturnsCorrectItem() {
    Item item = new Vehicle("v2", "Toyota", 8000.0);
    auctionService.addAuction(new Auction("a-item-1", item, DURATION));
    assertEquals(item, auctionService.getItemInAuction("a-item-1"));
  }
 
  @Test
  void getItemInAuctionNonExistentIdReturnsNull() {
    assertNull(auctionService.getItemInAuction("ghost-id"));
  }
 
  @Test
  void getAuctionsMapNotNull() {
    assertNotNull(auctionService.getAuctionsMap());
  }
 
  @Test
  void setAuctionsNullDoesNotThrow() {
    assertDoesNotThrow(() -> auctionService.setAuctions(null));
  }
 
  // ================================================================
  // UserManager — Singleton
  // ================================================================
 
  @Test
  void userManagerGetInstanceReturnsSameObject() {
    assertSame(UserManager.getInstance(), UserManager.getInstance());
  }
 
  // ================================================================
  // UserManager — default admin
  // ================================================================
 
  @Test
  void defaultAdminExistsAfterInit() {
    assertNotNull(userManager.findUserById("admin"));
  }
 
  @Test
  void defaultAdminCanLoginWithDefaultCredentials() {
    assertNotNull(userManager.login("admin", "admin123"));
  }
 
  @Test
  void defaultAdminHasAdminRole() {
    assertInstanceOf(Admin.class, userManager.findUserById("admin"));
  }
 
  // ================================================================
  // UserManager — register
  // ================================================================
 
  @Test
  void registerNewUsernameReturnsTrue() {
    assertTrue(userManager.register("alice", "pw123", "BIDDER"));
  }
 
  @Test
  void registerDuplicateUsernameReturnsFalse() {
    userManager.register("bob", "pw", "BIDDER");
    assertFalse(userManager.register("bob", "other", "SELLER"));
  }
 
  @Test
  void registerRoleAdminCreatesAdminInstance() {
    userManager.register("newadmin", "pw", "ADMIN");
    assertInstanceOf(Admin.class, userManager.findUserById("newadmin"));
  }
 
  @Test
  void registerRoleSellerCreatesSellerInstance() {
    userManager.register("seller1", "pw", "SELLER");
    assertInstanceOf(Seller.class, userManager.findUserById("seller1"));
  }
 
  @Test
  void registerRoleBidderCreatesBidderInstance() {
    userManager.register("bidder1", "pw", "BIDDER");
    assertInstanceOf(Bidder.class, userManager.findUserById("bidder1"));
  }
 
  @Test
  void registerUnknownRoleCreatesBidderByDefault() {
    userManager.register("guest1", "pw", "GUEST");
    assertInstanceOf(Bidder.class, userManager.findUserById("guest1"));
  }
 
  // ================================================================
  // UserManager — login
  // ================================================================
 
  @Test
  void loginCorrectPasswordReturnsUserObject() {
    userManager.register("carol", "secret", "BIDDER");
    assertNotNull(userManager.login("carol", "secret"));
  }
 
  @Test
  void loginWrongPasswordReturnsNull() {
    userManager.register("dave", "correct", "BIDDER");
    assertNull(userManager.login("dave", "wrong"));
  }
 
  @Test
  void loginNonExistentUsernameReturnsNull() {
    assertNull(userManager.login("ghost", "pw"));
  }
 
  @Test
  void loginCorrectPasswordReturnsUserWithCorrectUsername() {
    userManager.register("eve", "pw", "SELLER");
    User user = userManager.login("eve", "pw");
    assertNotNull(user);
    assertEquals("eve", user.getUsername());
  }
 
  // ================================================================
  // UserManager — findUserById
  // ================================================================
 
  @Test
  void findUserByIdExistingReturnsUser() {
    userManager.register("frank", "pw", "BIDDER");
    assertNotNull(userManager.findUserById("frank"));
  }
 
  @Test
  void findUserByIdNonExistentReturnsNull() {
    assertNull(userManager.findUserById("nobody"));
  }
}