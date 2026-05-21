package com.auction.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.controller.network.ClientHandler;
import com.auction.model.entities.user.Bidder;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.auction.service.usermanger.UserManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho ClientHandler (kiến trúc Command Pattern mới).
 *
 * <p>API mới: ClientHandler.run() đọc lệnh từ socket -> dispatch tới Command tương ứng
 * trong commandMap. Không còn các method handleLogin/handleBid/... gọi trực tiếp.
 *
 * <p>Chiến lược test:
 * <ul>
 *   <li><b>Direct API</b>: test getCurrentUser/setCurrentUser/sendMessage/validatePayload
 *       trên handler trực tiếp. Inject {@code out} qua reflection để sendMessage hoạt
 *       động khi không chạy run() (run() mới là nơi out được khởi tạo).</li>
 *   <li><b>Dispatch qua run()</b>: chạy run() trên thread riêng, gửi lệnh qua socket,
 *       đọc response từ phía client -> kiểm tra ClientHandler dispatch đúng Command
 *       và Command phản hồi đúng định dạng.</li>
 * </ul>
 */
public class ClientHandlerTest {

  // =========================================================================
  // HẠ TẦNG TEST
  // =========================================================================

  private ServerSocket serverSocket;
  private Socket clientSide;    // phía test: ghi command, đọc response
  private Socket serverSide;    // phía ClientHandler cầm
  private ClientHandler handler;
  private BufferedReader clientIn;
  private PrintWriter clientOut;

  private AuctionService auctionService;
  private UserManager userManager;

  // Thread chạy run() khi test cần dispatch loop. Null nếu test không dùng.
  private Thread runThread;

  @BeforeEach
  void setUp() throws Exception {
    // Reset state. UserManager/DataManager dùng Holder idiom -> clear bằng API.
    UserManager.getInstance().setUsers(new HashMap<>());
    resetSingletonIfExists(AuctionService.class, "instance");

    userManager = UserManager.getInstance();
    userManager.initDefaultData(); // tạo admin/admin123
    auctionService = AuctionService.getInstance();

    // Cặp socket localhost với cổng ngẫu nhiên (tránh xung đột)
    serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();

    final Socket[] acceptHolder = new Socket[1];
    Thread acceptThread = new Thread(() -> {
      try {
        acceptHolder[0] = serverSocket.accept();
      } catch (IOException ignored) {
        // socket bị đóng trong tearDown
      }
    });
    acceptThread.setDaemon(true);
    acceptThread.start();

    clientSide = new Socket("localhost", port);
    acceptThread.join(2000);
    serverSide = acceptHolder[0];

    handler = new ClientHandler(serverSide);

    // I/O phía test
    clientIn = new BufferedReader(new InputStreamReader(clientSide.getInputStream()));
    clientOut = new PrintWriter(clientSide.getOutputStream(), true);
  }

  @AfterEach
  void tearDown() {
    // Đóng socket trước để run() thoát khỏi readLine()
    safeClose(clientSide);
    safeClose(serverSide);
    safeClose(serverSocket);
    if (runThread != null) {
      try {
        runThread.join(1500);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
    }
    new File("auctions.dat").delete();
    new File("users.dat").delete();
    new File("auctions.dat.tmp").delete();
    new File("users.dat.tmp").delete();
  }

  // =========================================================================
  // HELPERS
  // =========================================================================

  /**
   * Inject {@code out} vào handler để sendMessage hoạt động khi không chạy run().
   * Field {@code out} của ClientHandler chỉ được khởi tạo trong run() - với các
   * test gọi method trực tiếp, ta gán out bằng reflection.
   */
  private void injectOut() throws Exception {
    PrintWriter out = new PrintWriter(serverSide.getOutputStream(), true);
    Field outField = ClientHandler.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(handler, out);
  }

  /**
   * Bật run() trên thread daemon. Sau lần gọi này, mọi dòng test ghi vào
   * clientOut sẽ được handler đọc và dispatch.
   */
  private void startRunLoop() {
    runThread = new Thread(handler);
    runThread.setDaemon(true);
    runThread.start();
    // Cho run() vài chục ms để init in/out trước khi test ghi lệnh
    try {
      Thread.sleep(50);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  /** Đọc 1 dòng response với timeout 1500ms; null nếu hết giờ. */
  private String readResponse() throws IOException {
    long deadline = System.currentTimeMillis() + 1500;
    while (System.currentTimeMillis() < deadline) {
      if (clientIn.ready()) {
        return clientIn.readLine();
      }
      try {
        Thread.sleep(20);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
        return null;
      }
    }
    return null;
  }

  /** Gửi lệnh qua socket (yêu cầu đã startRunLoop). */
  private void sendCommand(String line) {
    clientOut.println(line);
  }

  /** Reset singleton nếu field 'instance' còn tồn tại (chỉ áp dụng cho non-Holder). */
  private static void resetSingletonIfExists(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(null, null);
    } catch (NoSuchFieldException e) {
      // Class đã chuyển sang Holder idiom - bỏ qua
    } catch (Exception ignored) {
      // ignore
    }
  }

  private static void safeClose(java.io.Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException ignored) {
        // best-effort
      }
    }
  }

  // =========================================================================
  // 1. GETTER / SETTER CƠ BẢN (không cần out, không cần run)
  // =========================================================================

  @Test
  void getCurrentUserInitiallyNull() {
    assertNull(handler.getCurrentUser(),
        "Chưa login thì currentUser phải null");
  }

  @Test
  void setAndGetCurrentUserRoundtrip() {
    Bidder bidder = new Bidder("alice", "hash", "alice@test.com");
    handler.setCurrentUser(bidder);
    assertEquals(bidder, handler.getCurrentUser(),
        "setCurrentUser rồi getCurrentUser phải trả về cùng object");
  }

  @Test
  void getAssociatedUsernameNullWhenNotLoggedIn() {
    assertNull(handler.getAssociatedUsername(),
        "Chưa login thì associatedUsername phải null");
  }

  @Test
  void getAssociatedUsernameReturnsUsernameAfterSetUser() {
    handler.setCurrentUser(new Bidder("bob", "hash", "bob@test.com"));
    assertEquals("bob", handler.getAssociatedUsername(),
        "Sau setCurrentUser, associatedUsername phải = username của user");
  }

  @Test
  void setCurrentUserToNullClearsState() {
    handler.setCurrentUser(new Bidder("charlie", "hash", "c@test.com"));
    handler.setCurrentUser(null);
    assertNull(handler.getCurrentUser(),
        "setCurrentUser(null) phải clear currentUser");
    assertNull(handler.getAssociatedUsername());
  }

  // =========================================================================
  // 2. sendMessage / update / validatePayload (cần out)
  // =========================================================================

  @Test
  void sendMessageDeliversSingleLineThroughSocket() throws Exception {
    injectOut();
    handler.sendMessage("HELLO_TEST");
    String response = readResponse();
    assertNotNull(response, "sendMessage phải gửi được dữ liệu qua socket");
    assertEquals("HELLO_TEST", response);
  }

  @Test
  void updateActsAsSendMessage() throws Exception {
    injectOut();
    handler.update("NOTI_TEST|data");
    String response = readResponse();
    assertNotNull(response, "update() phải gửi được dữ liệu qua socket");
    assertEquals("NOTI_TEST|data", response);
  }

  @Test
  void sendMessageHandlesNullOutGracefully() {
    // out chưa init (chưa inject, chưa chạy run) - sendMessage phải không crash
    handler.sendMessage("anything");
    // không assertion, chỉ cần không ném exception
  }

  @Test
  void validatePayloadReturnsTrueWhenLengthSufficient() throws Exception {
    injectOut();
    String[] parts = {"CMD", "arg1", "arg2"};
    assertTrue(handler.validatePayload(parts, 3),
        "parts đủ độ dài phải trả về true");
  }

  @Test
  void validatePayloadReturnsFalseAndSendsErrorWhenInsufficient() throws Exception {
    injectOut();
    String[] parts = {"CMD"};
    assertFalse(handler.validatePayload(parts, 3),
        "parts thiếu phải trả về false");
    String response = readResponse();
    assertNotNull(response, "Thiếu parts phải gửi ERROR message");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Response phải bắt đầu bằng ERROR, nhận được: " + response);
  }

  @Test
  void validatePayloadReturnsFalseForNullParts() throws Exception {
    injectOut();
    assertFalse(handler.validatePayload(null, 1),
        "null parts phải trả về false");
  }

  // =========================================================================
  // 3. DISPATCH QUA run() - UNKNOWN COMMAND
  // =========================================================================

  @Test
  void runReturnsErrorForUnknownCommand() throws Exception {
    startRunLoop();
    sendCommand("INVALID_COMMAND|x");
    String response = readResponse();
    assertNotNull(response, "Lệnh không hợp lệ phải nhận phản hồi");
    assertTrue(response.startsWith(Protocol.ERROR),
        "Lệnh không hợp lệ phải trả ERROR, nhận: " + response);
  }

 
  // =========================================================================
  // 4. DISPATCH QUA run() - LOGIN
  // =========================================================================

  @Test
  void runDispatchesLoginSuccessForDefaultAdmin() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    String response = readResponse();
    assertNotNull(response, "Login đúng phải có phản hồi");
    assertTrue(response.startsWith(Protocol.RES_LOGIN_SUCCESS),
        "Login admin/admin123 phải thành công, nhận: " + response);
  }

  @Test
  void runDispatchesLoginFailsWithWrongPassword() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|sai_mat_khau");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_LOGIN_FAILED),
        "Sai mật khẩu phải trả LOGIN_FAILED, nhận: " + response);
  }

  @Test
  void runDispatchesLoginFailsForUnknownUser() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|nguoi_la|pw");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_LOGIN_FAILED),
        "User không tồn tại phải trả LOGIN_FAILED, nhận: " + response);
  }

  @Test
  void runDispatchesLoginFailsWhenPayloadTooShort() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Login thiếu password phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 5. DISPATCH QUA run() - REGISTER
  // =========================================================================

  @Test
  void runDispatchesRegisterFailsWhenPayloadTooShort() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_REGISTER + "|only_user");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Register thiếu tham số phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesRegisterSuccessForNewUser() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_REGISTER + "|newbie|pw|BIDDER|newbie@test.com");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_REGISTER_SUCCESS),
        "Đăng ký user mới phải thành công, nhận: " + response);
  }

  // =========================================================================
  // 6. DISPATCH QUA run() - LIST AUCTIONS (không cần login)
  // =========================================================================

  @Test
  void runDispatchesListAuctionsWorksWithoutLogin() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LIST_AUCTIONS);
    String response = readResponse();
    assertNotNull(response, "List auctions phải có phản hồi dù chưa login");
    assertFalse(response.startsWith(Protocol.ERROR),
        "List auctions không cần login, không được trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 7. DISPATCH QUA run() - DEPOSIT (yêu cầu login)
  // =========================================================================

  @Test
  void runDispatchesDepositFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_DEPOSIT + "|500000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Deposit chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesDepositSuccessAfterLogin() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse(); // consume login response

    sendCommand(Protocol.CMD_DEPOSIT + "|100000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_DEPOSIT_SUCCESS),
        "Deposit hợp lệ sau login phải thành công, nhận: " + response);
  }

  @Test
  void runDispatchesDepositFailsForNegativeAmount() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_DEPOSIT + "|-500");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Số tiền âm phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesDepositFailsForNonNumericAmount() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_DEPOSIT + "|abc");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Số tiền không phải số phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 8. DISPATCH QUA run() - GET_BALANCE
  // =========================================================================

  @Test
  void runDispatchesGetBalanceFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_GET_BALANCE);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "GetBalance chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesGetBalanceReturnsInfoAfterLogin() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_GET_BALANCE);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_BALANCE_INFO),
        "GetBalance sau login phải trả BALANCE_INFO, nhận: " + response);
  }

  // =========================================================================
  // 9. DISPATCH QUA run() - CREATE_AUCTION (phân quyền)
  // =========================================================================

  @Test
  void runDispatchesCreateAuctionFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    // 7 parts: CMD|type|name|price|duration|desc|image
    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ELECTRONICS|Laptop|5000000|60|desc|img");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Create auction chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesCreateAuctionFailsForBidderRole() throws Exception {
    userManager.register("bidder01", "pw", "BIDDER", "bidder01@test.com");
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|bidder01|pw");
    readResponse();

    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ELECTRONICS|Laptop|5000000|60|desc|img");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bidder không được tạo auction, nhận: " + response);
  }

  @Test
  void runDispatchesCreateAuctionSuccessForAdmin() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ELECTRONICS|LaptopTest|5000000|60|desc|img");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_SUCCESS),
        "Admin tạo auction phải thành công, nhận: " + response);
  }

  @Test
  void runDispatchesCreateAuctionSuccessForSeller() throws Exception {
    userManager.register("seller01", "pw", "SELLER", "seller01@test.com");
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|seller01|pw");
    readResponse();

    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ART|TranhSonDau|1000000|30|desc|img");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.RES_SUCCESS),
        "Seller tạo auction phải thành công, nhận: " + response);
  }

  @Test
  void runDispatchesCreateAuctionFailsForInvalidPrice() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ELECTRONICS|Laptop|NOT_A_NUMBER|60|desc|img");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Giá không hợp lệ phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesCreateAuctionFailsWhenPayloadTooShort() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_CREATE_AUCTION + "|ELECTRONICS");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Create auction thiếu parts phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 10. DISPATCH QUA run() - END / DELETE AUCTION (phân quyền)
  // =========================================================================

  @Test
  void runDispatchesEndAuctionFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_END_AUCTION + "|AUC_FAKE");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "End auction chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesDeleteAuctionFailsForBidder() throws Exception {
    userManager.register("bidder02", "pw", "BIDDER", "bidder02@test.com");
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|bidder02|pw");
    readResponse();

    sendCommand(Protocol.CMD_DELETE_AUCTION + "|AUC_FAKE");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bidder không được xoá auction, nhận: " + response);
  }

  // =========================================================================
  // 11. DISPATCH QUA run() - BID
  // =========================================================================

  @Test
  void runDispatchesBidFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_BID + "|AUC_FAKE|500000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bid chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesBidFailsForNonExistentAuction() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_BID + "|AUC_NOT_EXIST|500000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bid auction không tồn tại phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesBidFailsForNegativeAmount() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_BID + "|AUC_FAKE|-100");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bid số tiền âm phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesBidFailsForNonNumericAmount() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_BID + "|AUC_FAKE|notANumber");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bid không phải số phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesBidFailsWhenPayloadTooShort() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_BID + "|AUC_FAKE");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Bid thiếu amount phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 12. DISPATCH QUA run() - WATCH / GET_WATCHLIST / ADD_AUTO_BID
  // =========================================================================

  @Test
  void runDispatchesWatchFailsForNonBidderRole() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123"); // admin không phải Bidder
    readResponse();

    sendCommand(Protocol.CMD_WATCH + "|AUC_FAKE");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Chỉ Bidder mới được watch, nhận: " + response);
  }

  @Test
  void runDispatchesUnwatchFailsWhenPayloadTooShort() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_UNWATCH);
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "Unwatch thiếu auctionId phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesAddAutoBidFailsWhenNotLoggedIn() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_ADD_AUTO_BID + "|AUC_FAKE|1000000|50000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "AutoBid chưa login phải trả ERROR, nhận: " + response);
  }

  @Test
  void runDispatchesAddAutoBidFailsForNonExistentAuction() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    sendCommand(Protocol.CMD_ADD_AUTO_BID + "|AUC_NOT_EXIST|1000000|50000");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "AutoBid auction không tồn tại phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 13. DISPATCH QUA run() - GET_HISTORY
  // =========================================================================

  @Test
  void runDispatchesGetHistoryFailsForNonExistentAuction() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_GET_HISTORY + "|AUC_NOT_EXIST");
    String response = readResponse();
    assertNotNull(response);
    assertTrue(response.startsWith(Protocol.ERROR),
        "GetHistory auction không tồn tại phải trả ERROR, nhận: " + response);
  }

  // =========================================================================
  // 14. STATE FLOW - login -> setCurrentUser được set qua dispatch
  // =========================================================================

  @Test
  void runLoginSetsCurrentUserOnHandler() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|admin123");
    readResponse();

    // run() đang chạy; ClientHandler.currentUser được set qua LoginCommand
    assertNotNull(handler.getCurrentUser(),
        "Sau login thành công, handler.getCurrentUser() phải khác null");
    assertEquals("admin", handler.getCurrentUser().getUsername());
  }

  @Test
  void runLoginFailureDoesNotSetCurrentUser() throws Exception {
    startRunLoop();
    sendCommand(Protocol.CMD_LOGIN + "|admin|sai");
    readResponse();

    assertNull(handler.getCurrentUser(),
        "Login sai phải KHÔNG set currentUser");
  }
}
