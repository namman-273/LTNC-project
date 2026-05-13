package com.auction.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;
import com.auction.service.UserManager;
import com.auction.util.core.DataManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho ClientHandler.
 *
 * <p>Vấn đề cốt lõi: PrintWriter "out" trong ClientHandler chỉ được tạo bên trong run().
 * Các test gọi handle* trực tiếp (không qua run()) nên "out" = null → sendMessage() im lặng.
 * Giải pháp: dùng reflection để inject "out" vào handler ngay sau khi tạo,
 * trỏ vào outputStream của serverSide socket — test đọc từ clientSide như bình thường.
 */
public class ClientHandlerTest {

  // =========================================================================
  // HẠ TẦNG TEST
  // =========================================================================

  private ServerSocket serverSocket;
  private Socket clientSide;    // phía test đọc response
  private Socket serverSide;    // phía ClientHandler cầm
  private ClientHandler handler;
  private BufferedReader clientIn; // test đọc từ đây

  private AuctionService auctionService;
  private UserManager userManager;

  @BeforeEach
  void setUp() throws Exception {
    // Reset Singleton để mỗi test độc lập
    resetSingleton(UserManager.class, "instance");
    resetSingleton(AuctionService.class, "instance");
    resetSingleton(DataManager.class, "instance");

    userManager = UserManager.getInstance();
    userManager.initDefaultData(); // tạo admin mặc định

    auctionService = AuctionService.getInstance();

    // Tạo cặp socket thật qua localhost (cổng ngẫu nhiên, tránh xung đột)
    serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();

    // Thread chờ accept phía server
    final Socket[] acceptHolder = new Socket[1];
    Thread acceptThread = new Thread(() -> {
      try {
        acceptHolder[0] = serverSocket.accept();
      } catch (IOException ignored) {}
    });
    acceptThread.setDaemon(true);
    acceptThread.start();

    clientSide = new Socket("localhost", port);
    acceptThread.join(2000);
    serverSide = acceptHolder[0];

    // Tạo ClientHandler với serverSide socket
    handler = new ClientHandler(serverSide);

    // *** KEY FIX: inject PrintWriter "out" vào handler bằng reflection ***
    // ClientHandler chỉ init "out" trong run() — ta làm thay để handle* có thể gửi message
    PrintWriter injectedOut = new PrintWriter(serverSide.getOutputStream(), true);
    Field outField = ClientHandler.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(handler, injectedOut);

    // Test đọc response từ clientSide
    clientIn = new BufferedReader(new InputStreamReader(clientSide.getInputStream()));
  }

  @AfterEach
  void tearDown() throws Exception {
    if (clientIn   != null) try { clientIn.close();   } catch (IOException ignored) {}
    if (clientSide  != null && !clientSide.isClosed())  try { clientSide.close();  } catch (IOException ignored) {}
    if (serverSide  != null && !serverSide.isClosed())  try { serverSide.close();  } catch (IOException ignored) {}
    if (serverSocket != null && !serverSocket.isClosed()) try { serverSocket.close(); } catch (IOException ignored) {}
    new File("auctions.dat").delete();
    new File("users.dat").delete();
    new File("auctions.dat.tmp").delete();
    new File("users.dat.tmp").delete();
  }

  /** Reset field static (Singleton) về null giữa các test. */
  private static void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    Field f = clazz.getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(null, null);
  }

  /**
   * Đọc một dòng response với timeout 1500ms.
   * Trả về null nếu không có gì đến trong thời gian đó.
   */
  private String readResponse() throws IOException {
    long deadline = System.currentTimeMillis() + 1500;
    while (System.currentTimeMillis() < deadline) {
      if (clientIn.ready()) {
        return clientIn.readLine();
      }
      try { Thread.sleep(20); } catch (InterruptedException ignored) {}
    }
    return null;
  }

  /** Login nhanh với admin mặc định; bỏ qua response. */
  private void loginAsAdmin() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin", "admin123"},
        auctionService);
    readResponse(); // consume response, không dùng
  }

  // =========================================================================
  // GETTER / SETTER CƠ BẢN
  // =========================================================================

  @Test
  void getCurrentUserInitiallyNull() {
    assertNull(handler.getCurrentUser(),
        "Chưa login thì currentUser phải null");
  }

  @Test
  void setAndGetCurrentUser() {
    Bidder bidder = new Bidder("alice", "hash", null);
    handler.setCurrentUser(bidder);
    assertEquals(bidder, handler.getCurrentUser());
  }

  @Test
  void getAssociatedUsernameNullWhenNotLoggedIn() {
    assertNull(handler.getAssociatedUsername());
  }

  @Test
  void getAssociatedUsernameReturnsUsernameAfterSetUser() {
    handler.setCurrentUser(new Bidder("bob", "hash", null));
    assertEquals("bob", handler.getAssociatedUsername());
  }

  // =========================================================================
  // validatePayload
  // =========================================================================

  @Test
  void validatePayloadReturnsTrueWhenSufficient() throws IOException {
    String[] parts = {"CMD", "arg1", "arg2"};
    assertTrue(handler.validatePayload(parts, 3));
  }

  @Test
  void validatePayloadReturnsFalseAndSendsErrorWhenInsufficient() throws IOException {
    String[] parts = {"CMD"};
    boolean result = handler.validatePayload(parts, 3);
    assertFalse(result, "Phải trả false khi thiếu tham số");

    String response = readResponse();
    assertNotNull(response, "validatePayload phải gửi ERROR message khi thiếu tham số");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Phải gửi ERROR khi thiếu tham số, nhận được: " + response);
  }

  @Test
  void validatePayloadReturnsFalseForNull() throws IOException {
    boolean result = handler.validatePayload(null, 1);
    assertFalse(result, "null parts phải trả về false");
  }

  // =========================================================================
  // sendMessage & update (AuctionParticipant)
  // =========================================================================

  @Test
  void sendMessageDeliversSingleLine() throws IOException {
    handler.sendMessage("HELLO_TEST");
    String response = readResponse();
    assertNotNull(response, "sendMessage phải gửi được dữ liệu qua socket");
    assertEquals("HELLO_TEST", response);
  }

  @Test
  void updateDeliversSameAsSendMessage() throws IOException {
    handler.update("NOTI_TEST|data");
    String response = readResponse();
    assertNotNull(response, "update() phải gửi được dữ liệu qua socket");
    assertEquals("NOTI_TEST|data", response);
  }

  @Test
  void sendMessageHandlesNullGracefully() {
    // Không được ném exception khi gửi null
    handler.sendMessage(null);
  }

  // =========================================================================
  // handleRegister
  // =========================================================================

  
  @Test
  void handleRegisterFailsWhenPayloadTooShort() throws IOException {
    handler.handleRegister(new String[]{Protocol.CMD_REGISTER, "only_user"});
    String response = readResponse();
    assertNotNull(response, "Thiếu tham số phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Thiếu tham số phải trả về ERROR, nhận được: " + response);
  }

  // =========================================================================
  // handleLogin
  // =========================================================================

  @Test
  void handleLoginSuccessWithDefaultAdmin() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin", "admin123"},
        auctionService);
    String response = readResponse();
    assertNotNull(response, "handleLogin phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_LOGIN_SUCCESS),
        "Login đúng credentials phải thành công, nhận được: " + response);
  }

  @Test
  void handleLoginSetsCurrentUser() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin", "admin123"},
        auctionService);
    readResponse();
    assertNotNull(handler.getCurrentUser());
    assertEquals("admin", handler.getCurrentUser().getUsername());
  }

  @Test
  void handleLoginFailsWithWrongPassword() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin", "wrongpass"},
        auctionService);
    String response = readResponse();
    assertNotNull(response, "Sai mật khẩu phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_LOGIN_FAILED),
        "Sai mật khẩu phải trả về LOGIN_FAILED, nhận được: " + response);
  }

  @Test
  void handleLoginFailsWithUnknownUser() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "nobody", "pass"},
        auctionService);
    String response = readResponse();
    assertNotNull(response, "User không tồn tại phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_LOGIN_FAILED),
        "User không tồn tại phải trả về LOGIN_FAILED, nhận được: " + response);
  }

  @Test
  void handleLoginFailsWhenPayloadTooShort() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  @Test
  void handleLoginDoesNotSetCurrentUserOnFailure() throws IOException {
    handler.handleLogin(
        new String[]{Protocol.CMD_LOGIN, "admin", "wrong"},
        auctionService);
    readResponse();
    assertNull(handler.getCurrentUser(),
        "currentUser phải giữ null khi login thất bại");
  }

  // =========================================================================
  // handleListAuctions
  // =========================================================================

  @Test
  void handleListAuctionsReturnsListSuccess() throws IOException {
    handler.handleListAuctions(auctionService);
    String response = readResponse();
    assertNotNull(response, "handleListAuctions phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_LIST_SUCCESS),
        "LIST_AUCTIONS phải trả về LIST_AUCTIONS_SUCCESS, nhận được: " + response);
  }

  @Test
  void handleListAuctionsWorksWhenNotLoggedIn() throws IOException {
    assertNull(handler.getCurrentUser());
    handler.handleListAuctions(auctionService);
    String response = readResponse();
    assertNotNull(response, "handleListAuctions phải gửi phản hồi dù chưa login");
    assertFalse(response.startsWith(Protocol.ERROR),
        "List auction không cần login, không được trả ERROR, nhận được: " + response);
  }

  // =========================================================================
  // handleDeposit
  // =========================================================================

  @Test
  void handleDepositFailsWhenNotLoggedIn() throws IOException {
    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT, "500000"});
    String response = readResponse();
    assertNotNull(response, "Nạp tiền chưa login phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Nạp tiền mà chưa login phải báo ERROR, nhận được: " + response);
  }

  @Test
  void handleDepositSuccessWhenLoggedIn() throws IOException {
    loginAsAdmin();
    double balanceBefore = handler.getCurrentUser().getBalance();

    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT, "100000"});
    String response = readResponse();
    assertNotNull(response, "Nạp tiền hợp lệ phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_DEPOSIT_SUCCESS),
        "Nạp tiền hợp lệ phải trả về DEPOSIT_SUCCESS, nhận được: " + response);
    assertEquals(balanceBefore + 100000, handler.getCurrentUser().getBalance(), 0.01);
  }

  @Test
  void handleDepositFailsForNegativeAmount() throws IOException {
    loginAsAdmin();
    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT, "-500"});
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Số tiền âm phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleDepositFailsForZeroAmount() throws IOException {
    loginAsAdmin();
    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT, "0"});
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Số tiền = 0 phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleDepositFailsForNonNumericAmount() throws IOException {
    loginAsAdmin();
    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT, "abc"});
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chuỗi không phải số phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleDepositFailsWhenPayloadTooShort() throws IOException {
    loginAsAdmin();
    handler.handleDeposit(new String[]{Protocol.CMD_DEPOSIT});
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  // =========================================================================
  // handleGetBalance
  // =========================================================================

  @Test
  void handleGetBalanceFailsWhenNotLoggedIn() throws IOException {
    handler.handleGetBalance();
    String response = readResponse();
    assertNotNull(response, "GetBalance chưa login phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chưa login phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleGetBalanceReturnsBalanceInfoWhenLoggedIn() throws IOException {
    loginAsAdmin();
    handler.handleGetBalance();
    String response = readResponse();
    assertNotNull(response, "GetBalance đã login phải gửi phản hồi");
    assertTrue(response.startsWith(Protocol.RES_BALANCE_INFO),
        "Đã login phải nhận được BALANCE_INFO, nhận được: " + response);
  }

  // =========================================================================
  // handleCreateAuction
  // =========================================================================

  @Test
  void handleCreateAuctionFailsWhenNotLoggedIn() throws IOException {
    // CreateAuctionCommand yêu cầu 7 parts: CMD|type|name|price|duration|desc|imageUrl
    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ELECTRONICS", "Laptop", "5000000", "60", "", ""},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  @Test
  void handleCreateAuctionFailsForBidderRole() throws IOException {
    userManager.register("bidder01", "pass", "BIDDER", null);
    handler.handleLogin(new String[]{Protocol.CMD_LOGIN, "bidder01", "pass"}, auctionService);
    readResponse();

    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ELECTRONICS", "Laptop", "5000000", "60", "", ""},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bidder không có quyền tạo auction, nhận được: " + response);
  }

  @Test
  void handleCreateAuctionSuccessForAdmin() throws IOException {
    loginAsAdmin();
    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ELECTRONICS", "Laptop Test", "5000000", "60", "", ""},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_SUCCESS),
        "Admin tạo auction hợp lệ phải thành công, nhận được: " + response);
  }

  @Test
  void handleCreateAuctionSuccessForSeller() throws IOException {
    userManager.register("seller01", "pass", "SELLER", null);
    handler.handleLogin(new String[]{Protocol.CMD_LOGIN, "seller01", "pass"}, auctionService);
    readResponse();

    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ART", "Tranh Son Dau", "1000000", "30", "", ""},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_SUCCESS),
        "Seller tạo auction hợp lệ phải thành công, nhận được: " + response);
  }

  @Test
  void handleCreateAuctionFailsForInvalidPrice() throws IOException {
    loginAsAdmin();
    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ELECTRONICS", "Laptop", "INVALID_PRICE", "60", "", ""},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Giá không hợp lệ phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleCreateAuctionFailsWhenPayloadTooShort() throws IOException {
    loginAsAdmin();
    handler.handleCreateAuction(
        new String[]{Protocol.CMD_CREATE_AUCTION, "ELECTRONICS"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  // =========================================================================
  // handleEndAuction & handleDeleteAuction (kiểm tra phân quyền)
  // =========================================================================

  @Test
  void handleEndAuctionFailsWhenNotLoggedIn() throws IOException {
    handler.handleEndAuction(
        new String[]{Protocol.CMD_END_AUCTION, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  @Test
  void handleEndAuctionFailsForNonAdmin() throws IOException {
    userManager.register("seller02", "pass", "SELLER", null);
    handler.handleLogin(new String[]{Protocol.CMD_LOGIN, "seller02", "pass"}, auctionService);
    readResponse();

    handler.handleEndAuction(
        new String[]{Protocol.CMD_END_AUCTION, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Non-admin không được đóng phiên, nhận được: " + response);
  }

  @Test
  void handleDeleteAuctionFailsWhenNotLoggedIn() throws IOException {
    handler.handleDeleteAuction(
        new String[]{Protocol.CMD_DELETE_AUCTION, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  @Test
  void handleDeleteAuctionFailsForNonAdmin() throws IOException {
    userManager.register("bidder02", "pass", "BIDDER", null);
    handler.handleLogin(new String[]{Protocol.CMD_LOGIN, "bidder02", "pass"}, auctionService);
    readResponse();

    handler.handleDeleteAuction(
        new String[]{Protocol.CMD_DELETE_AUCTION, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bidder không được xóa phiên, nhận được: " + response);
  }

  // =========================================================================
  // handleBid
  // =========================================================================

  @Test
  void handleBidFailsWhenNotLoggedIn() throws IOException {
    handler.handleBid(
        new String[]{Protocol.CMD_BID, "AUC_FAKE", "500000"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chưa login không được đặt giá, nhận được: " + response);
  }

  @Test
  void handleBidFailsForNonExistentAuction() throws IOException {
    loginAsAdmin();
    handler.handleBid(
        new String[]{Protocol.CMD_BID, "AUC_NOT_EXIST", "500000"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Auction không tồn tại phải trả ERROR, nhận được: " + response);
  }

  @Test
  void handleBidFailsForNegativeAmount() throws IOException {
    loginAsAdmin();
    handler.handleBid(
        new String[]{Protocol.CMD_BID, "AUC_FAKE", "-100"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Giá âm phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleBidFailsForNonNumericAmount() throws IOException {
    loginAsAdmin();
    handler.handleBid(
        new String[]{Protocol.CMD_BID, "AUC_FAKE", "notANumber"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chuỗi không phải số phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleBidFailsWhenPayloadTooShort() throws IOException {
    loginAsAdmin();
    handler.handleBid(
        new String[]{Protocol.CMD_BID, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR));
  }

  // =========================================================================
  // handleGetHistory
  // =========================================================================

  @Test
  void handleGetHistoryFailsForNonExistentAuction() throws IOException {
    handler.handleGetHistory(
        new String[]{Protocol.CMD_GET_HISTORY, "AUC_NOT_EXIST"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Auction không tồn tại phải trả ERROR, nhận được: " + response);
  }

  @Test
  void handleWatchFailsForNonBidder() throws IOException {
    loginAsAdmin(); // Admin không phải Bidder
    handler.handleWatch(
        new String[]{Protocol.CMD_WATCH, "AUC_FAKE"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chỉ Bidder mới được watch, nhận được: " + response);
  }

  @Test
  void handleUnwatchFailsWhenPayloadTooShort() throws IOException {
    loginAsAdmin();
    handler.handleUnwatch(
        new String[]{Protocol.CMD_UNWATCH},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Thiếu tham số phải trả về ERROR, nhận được: " + response);
  }

  @Test
  void handleGetWatchlistDoesNotCrashWhenNotLoggedIn() {
    // Không crash — chỉ cần không ném exception
    handler.handleGetWatchlist(auctionService);
  }

  // =========================================================================
  // handleAddAutoBid
  // =========================================================================

  @Test
  void handleAddAutoBidFailsWhenNotLoggedIn() throws IOException {
    handler.handleAddAutoBid(
        new String[]{Protocol.CMD_ADD_AUTO_BID, "AUC_FAKE", "1000000", "50000"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chưa login không được đặt auto-bid, nhận được: " + response);
  }

  @Test
  void handleAddAutoBidFailsForNonExistentAuction() throws IOException {
    loginAsAdmin();
    handler.handleAddAutoBid(
        new String[]{Protocol.CMD_ADD_AUTO_BID, "AUC_NOT_EXIST", "1000000", "50000"},
        auctionService);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Auction không tồn tại phải trả ERROR, nhận được: " + response);
  }

  // =========================================================================
  // LỆNH KHÔNG HỢP LỆ — test qua luồng run() thật
  // =========================================================================

  @Test
  void unknownCommandReturnsError() throws Exception {
    // Test này dùng run() thật để out được init bên trong run()
    // Đóng cặp socket cũ đã inject trước (tránh resource leak)
    serverSide.close();
    clientSide.close();

    // Tạo cặp socket mới sạch, không inject out
    ServerSocket tmpServer = new ServerSocket(0);
    final Socket[] tmpAccepted = new Socket[1];
    Thread acc = new Thread(() -> {
      try { tmpAccepted[0] = tmpServer.accept(); } catch (IOException ignored) {}
    });
    acc.setDaemon(true);
    acc.start();

    Socket tmpClient = new Socket("localhost", tmpServer.getLocalPort());
    acc.join(2000);
    tmpServer.close();

    // Chạy handler trên thread riêng (run() sẽ init out từ socket stream)
    ClientHandler runHandler = new ClientHandler(tmpAccepted[0]);
    Thread runThread = new Thread(runHandler);
    runThread.setDaemon(true);
    runThread.start();

    PrintWriter tmpOut = new PrintWriter(tmpClient.getOutputStream(), true);
    BufferedReader tmpIn = new BufferedReader(new InputStreamReader(tmpClient.getInputStream()));

    // Gửi lệnh không hợp lệ
    tmpOut.println("INVALID_COMMAND|arg1");

    // Đọc response với timeout 2 giây
    String response = null;
    long deadline = System.currentTimeMillis() + 2000;
    while (System.currentTimeMillis() < deadline) {
      if (tmpIn.ready()) { response = tmpIn.readLine(); break; }
      Thread.sleep(20);
    }

    tmpClient.close();
    runThread.join(1000);

    assertNotNull(response, "Lệnh không hợp lệ phải nhận được phản hồi");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Lệnh không hợp lệ phải trả về ERROR, nhận được: " + response);
  }
}