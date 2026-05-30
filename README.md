# 🏆 Hệ thống Đấu giá Trực tuyến (Online Auction System)

> Bài tập lớn – Lập trình nâng cao (LTNC) · Học kỳ II, 2025–2026
> Kiến trúc **Client–Server** · **Java 17** · **JavaFX 17** · **MVC**

---

## 📋 Mục lục

- [Mô tả hệ thống](#-mô-tả-hệ-thống)
- [Công nghệ & Môi trường](#-công-nghệ--môi-trường)
- [Yêu cầu cài đặt](#-yêu-cầu-cài-đặt)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Vị trí các file .jar](#-vị-trí-các-file-jar)
- [Hướng dẫn chạy Server & Client](#-hướng-dẫn-chạy-server--client)
- [Kết nối liên máy tính (LAN)](#-kết-nối-liên-máy-tính-lan--nhiều-máy)
- [Chức năng đã hoàn thành](#-chức-năng-đã-hoàn-thành)

---

## 📖 Mô tả hệ thống

Hệ thống **Đấu giá Trực tuyến** là nền tảng phần mềm cho phép nhiều người dùng cùng cạnh tranh giá để mua một sản phẩm trong một khoảng thời gian xác định.

**Phạm vi hệ thống:**

- **Người dùng (Users):** Đăng ký, đăng nhập, quản lý tài khoản với 3 vai trò: `Bidder`, `Seller`, `Admin`.
- **Sản phẩm (Items):** Seller đăng sản phẩm thuộc 3 loại: `Electronics`, `Art`, `Vehicle`.
- **Phiên đấu giá (Auctions):** Mỗi phiên có thời hạn, giá khởi điểm, và vòng đời trạng thái rõ ràng (`OPEN → RUNNING → FINISHED → PAID / CANCELED`).
- **Đặt giá (Bidding):** Bidder đặt giá hợp lệ theo thời gian thực; hệ thống tự động xác định người thắng cuộc.
- **Thông báo realtime:** Mọi client đang xem phiên nhận cập nhật ngay lập tức khi có bid mới.

---

## 🛠 Công nghệ & Môi trường

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java **17+** |
| Giao diện (Client) | JavaFX **17.0.8** + FXML (SceneBuilder) |
| Kiến trúc mạng | Java Socket (TCP, port **9999**) |
| Lưu trữ dữ liệu | Java Serialization (`.dat` files) |
| Build tool | Maven **3.8+** (kèm sẵn Maven Wrapper `mvnw`) |
| Unit Test | JUnit **5.10.2** |
| Code coverage | JaCoCo **0.8.12** (tối thiểu 60%) |
| Coding convention | Checkstyle (Google Java Style) |
| CI/CD | GitHub Actions |

---

## ⚙️ Yêu cầu cài đặt

### Bước 1 – Cài đặt JDK 17+

```bash
java -version
javac -version
# Yêu cầu: Java 17 hoặc cao hơn
```

Tải tại: <https://adoptium.net/> hoặc <https://www.oracle.com/java/technologies/downloads/>.

### Bước 2 – (Tùy chọn) Cài đặt Maven 3.8+

Project đã **đi kèm sẵn Maven Wrapper** (`mvnw` / `mvnw.cmd`), nên bạn **không bắt buộc** phải cài Maven. Nếu muốn dùng `mvn` toàn cục, tải tại <https://maven.apache.org/download.cgi>.

> Các ví dụ bên dưới dùng `mvn`. Nếu bạn không cài Maven, hãy thay bằng `./mvnw` (Linux/macOS) hoặc `mvnw.cmd` (Windows).

### Bước 3 – Clone repository và build

```bash
git clone https://github.com/namman-273/LTNC-project.git
cd LTNC-project
mvn package -DskipTests
```

> ⚠️ **Lưu ý:** Mọi lệnh `mvn` và `java -jar` đều phải chạy từ bên trong thư mục `LTNC-project`. Nếu mở terminal mới, luôn `cd LTNC-project` trước.

> 💡 JavaFX 17 đã được khai báo dưới dạng Maven dependency trong `pom.xml`, nên bạn **không cần tải JavaFX SDK riêng**.

---

## 📁 Cấu trúc thư mục

```
LTNC-project/
├── pom.xml                              
├── mvnw, mvnw.cmd                     
├── README.md                          
├── .github/
│   └── workflows/
│       └── maven.yml                   
└── src/
    └── main/
        └── java/
            └── com/auction/
                ├── MainApp.java                   
                │
                ├── controller/
                │   ├── command/                   
                │   │   ├── AddAutoBidCommand.java
                │   │   ├── BidCommand.java
                │   │   ├── ChangePasswordCommand.java
                │   │   ├── ClientCommand.java
                │   │   ├── CreateAuctionCommand.java
                │   │   ├── DeleteAuctionCommand.java
                │   │   ├── DepositCommand.java
                │   │   ├── EndAuctionCommand.java
                │   │   ├── GetBalanceCommand.java
                │   │   ├── GetBidHistoryCommand.java
                │   │   ├── GetHistoryCommand.java
                │   │   ├── GetProfileCommand.java
                │   │   ├── GetWatchlistCommand.java
                │   │   ├── ListAuctionsCommand.java
                │   │   ├── LoginCommand.java
                │   │   ├── RegisterCommand.java
                │   │   ├── UnwatchCommand.java
                │   │   ├── UpdateEmailCommand.java
                │   │   └── WatchCommand.java
                │   ├── network/
                │   │   ├── ClientHandler.java
                │   │   └── ConnectionManager.java
                │   └── ui/                       
                │       ├── AdminDashboardController.java
                │       ├── AuctionListController.java
                │       ├── AuctionUtils.java                
                │       ├── AutoBidController.java
                │       ├── BalanceController.java
                │       ├── BaseController.java                
                │       ├── BidChartController.java
                │       ├── BidController.java
                │       ├── BidHistoryController.java
                │       ├── CreateAuctionController.java
                │       ├── LoginController.java
                │       ├── NotificationController.java
                │       ├── ProfileController.java
                │       ├── RegisterController.java
                │       ├── SellerController.java
                │       └── WatchlistController.java
                │
                ├── model/
                │   ├── entities/
                │   │   ├── Entity.java
                │   │   ├── Auction.java
                │   │   ├── BidTransaction.java
                │   │   ├── AutoBid.java
                │   │   ├── item/
                │   │   │   ├── Item.java
                │   │   │   ├── Art.java
                │   │   │   ├── Electronics.java
                │   │   │   └── Vehicle.java
                │   │   └── user/
                │   │       ├── User.java
                │   │       ├── Bidder.java
                │   │       ├── Seller.java
                │   │       └── Admin.java
                │   ├── auctionhelpers/
                │   │   ├── AuctionValidator.java
                │   │   ├── AuctionNotifier.java
                │   │   ├── AutoBidProcessor.java
                │   │   ├── AuctionFinancialProcessor.java
                │   │   ├── AuctionSnipingProcessor.java
                │   │   └── AuctionHelperFactory.java
                │   ├── dto/
                │   │   ├── AuctionRow.java
                │   │   └── BidHistoryEntry.java
                │   ├── enums/
                │   │   └── AuctionStatus.java    
                │   ├── factory/                   
                │   │   ├── ItemFactory.java
                │   │   ├── ItemFactoryRegistry.java
                │   │   ├── ArtFactory.java
                │   │   ├── ElectronicsFactory.java
                │   │   └── VehicleFactory.java
                │   └── observer/                  
                │       ├── Observer.java
                │       └── AuctionParticipant.java
                │
                ├── network/
                │   ├── client/
                │   │   └── ServerConnection.java  
                │   ├── protocol/
                │   │   └── Protocol.java          
                │   └── server/
                │       └── AuctionServer.java   
                │
                ├── service/
                │   ├── auctionservice/
                │   │   ├── AuctionService.java
                │   │   ├── AuctionRepository.java
                │   │   ├── AuctionScheduler.java
                │   │   ├── AuctionEndHandler.java
                │   │   ├── AuctionDeletionHandler.java                
                │   │   ├── AuctionFactory.java
                │   │   ├── AuctionNotificationService.java
                │   │   ├── AuctionDataPersistenceService.java
                │   │   ├── PaymentProcessor.java
                │   │   └── WatchlistService.java
                │   ├── bidhistorymanager/
                │   │   └── BidHistoryManager.java
                │   └── usermanger/              
                │       ├── AuthenticationService.java
                │       ├── DataPersistenceService.java
                │       ├── UserManager.java
                │       ├── UserFactory.java
                │       ├── UserRepository.java
                │       ├── UserValidator.java
                │       └── EmailIndexer.java
                │
                ├── util/
                │   ├── core/
                │   │   ├── SecurityUtils.java     
                │   │   ├── SessionManager.java   
                │   │   └── datamanager/         
                │   │       ├── DataManager.java          
                │   │       ├── DataPersistence.java     
                │   │       ├── FileDataPersistence.java
                │   │       ├── DataLoader.java
                │   │       ├── DataSaver.java
                │   │       ├── AutoSaveScheduler.java
                │   │       └── DirtyFlagTracker.java
                │   ├── exception/
                │   │   ├── InvalidBidException.java
                │   │   ├── AuctionClosedException.java
                │   │   └── AuthenticationException.java
                │   └── ui/
                │       ├── AlertUtil.java
                │       ├── NotificationManager.java
                │       └── ToastManager.java
                │
                └── views/
                    ├── fxml/                    
                    │   ├── LoginView.fxml
                    │   ├── RegisterView.fxml
                    │   ├── AuctionListView.fxml
                    │   ├── BidView.fxml
                    │   ├── BidChartView.fxml
                    │   ├── AutoBidView.fxml
                    │   ├── BidHistoryView.fxml
                    │   ├── SellerView.fxml
                    │   ├── CreateAuctionView.fxml
                    │   ├── AdminDashboardView.fxml
                    │   ├── ProfileView.fxml
                    │   ├── BalanceView.fxml
                    │   ├── WatchlistView.fxml
                    │   └── NotificationView.fxml
                    ├── java/                     
                    │   ├── LoginView.java
                    │   ├── RegisterView.java
                    │   ├── AuctionListView.java
                    │   ├── BidView.java
                    │   ├── BidChartView.java
                    │   ├── AutoBidView.java
                    │   ├── BidHistoryView.java
                    │   ├── SellerView.java
                    │   ├── CreateAuctionView.java
                    │   ├── AdminDashboardView.java
                    │   ├── ProfileView.java
                    │   ├── BalanceView.java
                    │   ├── WatchlistView.java
                    │   └── NotificationView.java
                    └── style/
                        └── style.css

src/test/java/com/auction/                         
├── model/                                        
│   ├── AntiSnipingTest.java
│   ├── AuctionBidIncrementTest.java
│   ├── AuctionConcurrencyTest.java
│   ├── AuctionCoreTest.java
│   ├── AuctionFinancialProcessorTest.java
│   ├── AuctionInputValidationTest.java
│   ├── AuctionObserverEdgeCaseTest.java
│   ├── AuctionRowTest.java
│   ├── AuctionSnipingProcessorTest.java
│   ├── AuctionStateMachineTest.java
│   ├── AuctionValidatorTest.java
│   ├── AutoBidProcessorTest.java
│   ├── BidTransactionTest.java
│   ├── BidderWatchlistTest.java
│   ├── CreateItemTest.java
│   ├── FactoryNullHandlingTest.java
│   ├── FactoryTest.java
│   ├── ItemTest.java
│   ├── ModelMiscTest.java
│   ├── UserBalanceTest.java
│   ├── UserEdgeCaseTest.java
│   └── UserTest.java
├── network/                                       
│   ├── ClientHandlerTest.java
│   └── ProtocolTest.java
├── service/                                        
│   ├── AuctionServiceTest.java
│   ├── CommandTest.java
│   ├── UserManagerTest.java
│   ├── UserManagerExtendedTest.java
│   └── UserManagerIntegrationTest.java
└── util/                                           
    ├── DataManagerRoundtripTest.java
    ├── ExceptionTest.java
    ├── SecurityAndSessionTest.java
    ├── SecurityUtilsTest.java
    └── SessionManagerTest.java
```

---

## 📦 Vị trí các file .jar

Sau khi build bằng `mvn package -DskipTests`, các file `.jar` được tạo trong thư mục `target/`:

```
target/
├── AuctionSystem-1.0-SNAPSHOT-shaded.jar   
├── AuctionSystem-1.0-SNAPSHOT.jar          
├── client.jar                              
├── server.jar                              
├── original-client.jar                     
└── original-server.jar                     
```

---

## ▶️ Hướng dẫn chạy Server & Client

### ⚠️ Quan trọng: Phải khởi động **Server trước**, **Client sau**.

### Bước 1 – Build project

```bash
cd LTNC-project
mvn package -DskipTests
```


### Bước 2 – Chạy Server

**Cách 1: Dùng Maven**

```bash
# Linux / macOS (bash)
cd LTNC-project
mvn exec:java -Dexec.mainClass=com.auction.network.server.AuctionServer

# Windows (PowerShell) — cần đóng tham số trong dấu nháy kép
cd LTNC-project
mvn exec:java "-Dexec.mainClass=com.auction.network.server.AuctionServer"
```

**Cách 2: Dùng file JAR**

```bash
cd LTNC-project
java -jar target/server.jar
```

**Kết quả mong đợi:**

```
╔════════════════════════════════════════════╗
║     🟢 SERVER AUCTION ĐANG CHẠY          ║
╠════════════════════════════════════════════╣
║  Port: 9999
║  IP máy này: 192.168.1.10
║
║  📝 Client hãy sửa server.properties:
║     server.host=192.168.1.10
║     server.port=9999
╚════════════════════════════════════════════╝

SERVER: Đang chạy trên cổng 9999
```

Server tự động in **IP của mình** ra console khi khởi động. Dữ liệu được lưu vào `auctions.dat`, `users.dat`, `history.dat` khi server tắt (và định kỳ qua `AutoSaveScheduler`).

### Bước 3 – Chạy Client (JavaFX)

Mở terminal **mới** (giữ server đang chạy), sau đó:

**Cách 1: Dùng Maven (khuyến nghị)**

```bash
cd LTNC-project
mvn javafx:run
```

**Cách 2: Dùng file JAR**

```bash
cd LTNC-project
java -jar target/client.jar
```

Cửa sổ đăng nhập sẽ hiện ra. Để chạy nhiều client cùng lúc, mở thêm terminal mới và chạy lại lệnh trên.

---

### 🌐 Kết nối liên máy tính (LAN / nhiều máy)

Hệ thống hỗ trợ sẵn kết nối liên máy qua file cấu hình `server.properties` 

#### Bước 1 – Khởi động Server, đọc IP

Chạy Server trên máy A. Console sẽ in ra IP của máy đó, ví dụ:

```
║  IP máy này: 192.168.1.10
║  📝 Client hãy sửa server.properties:
║     server.host=192.168.1.10
║     server.port=9999
```

#### Bước 2 – Sửa file `server.properties` trên máy Client

File `server.properties` đã có sẵn trong thư mục gốc `LTNC-project/`. Mở file và thay IP `localhost` bằng IP của máy Server in ra ở Bước 1:

```properties
# Trước (mặc định)
server.host=localhost
server.port=9999

# Sau (ví dụ IP máy Server là 192.168.1.10)
server.host=192.168.1.10
server.port=9999
```

Khi Client khởi động sẽ thông báo:

```
✓ Đã load config từ server.properties → Server: 192.168.1.10:9999
```

Nếu để nguyên `localhost`, Client chỉ kết nối được với Server trên cùng một máy.

#### Bước 3 – Mở port 9999 trên máy Server (nếu Client không kết nối được)

**Windows (PowerShell — chạy với quyền Administrator):**
```powershell
New-NetFirewallRule -DisplayName "AuctionServer 9999" `
    -Direction Inbound -Protocol TCP -LocalPort 9999 -Action Allow
```

**Linux (ufw):**
```bash
sudo ufw allow 9999/tcp
```

**macOS:** Vào **System Settings → Network → Firewall → Options**, thêm ngoại lệ cho Java hoặc tắt firewall tạm thời khi demo.

#### Sơ đồ kết nối

```
[Máy A – Server]                  [Máy B – Client]          [Máy C – Client]
  java -jar server.jar    ←TCP 9999─  java -jar client.jar    java -jar client.jar
  192.168.1.10:9999                   server.properties         server.properties
                                      server.host=192.168.1.10  server.host=192.168.1.10
```

> ⚠️ **Lưu ý:** Tất cả các máy phải cùng mạng LAN (cùng router/switch). Nếu demo qua Internet, cần cấu hình **Port Forwarding** trên router hoặc dùng VPN như ZeroTier, Tailscale.

---

### Tài khoản mặc định

| Vai trò | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |

---

## ✅ Chức năng đã hoàn thành

### Chức năng bắt buộc

| # | Chức năng | Trạng thái |
|---|---|---|
| 1 | **Quản lý người dùng** – Đăng ký / đăng nhập tài khoản | ✅ |
| 2 | **Phân quyền 3 vai trò** – Bidder, Seller, Admin | ✅ |
| 3 | **Quản lý sản phẩm** – Thêm / sửa / xóa sản phẩm (Art, Electronics, Vehicle) | ✅ |
| 4 | **Tạo phiên đấu giá** – Giá khởi điểm, thời gian bắt đầu & kết thúc | ✅ |
| 5 | **Tham gia đấu giá** – Đặt giá, kiểm tra hợp lệ, cập nhật người dẫn đầu | ✅ |
| 6 | **Kết thúc phiên tự động** – Xác định người thắng, chuyển trạng thái | ✅ |
| 7 | **Vòng đời phiên đấu giá** – `OPEN → RUNNING → FINISHED → PAID/CANCELED` | ✅ |
| 8 | **Xử lý lỗi & ngoại lệ** – `InvalidBidException`, `AuctionClosedException`, `AuthenticationException` | ✅ |
| 9 | **Giao diện GUI (JavaFX + FXML)** – 14 màn hình, áp dụng MVC | ✅ |
| 10 | **Xử lý đấu giá đồng thời** – `ReentrantLock`, tránh lost update, race condition | ✅ |
| 11 | **Realtime Update** – Observer Pattern qua Socket, push notification đến tất cả client | ✅ |
| 12 | **Kiến trúc Client–Server** – Java Socket TCP, port 9999 | ✅ |
| 13 | **MVC** – JavaFX + FXML cho Client; Controller–Service–Repository cho Server | ✅ |
| 14 | **Build tool Maven** – Quản lý dependencies, Checkstyle | ✅ |
| 15 | **Unit Test (JUnit 5) + JaCoCo coverage ≥ 60%** – 34 test classes bao phủ model / service / network / util | ✅ |
| 16 | **CI/CD (GitHub Actions)** – Tự động build, checkstyle, test, coverage khi push | ✅ |

### Chức năng nâng cao (Bonus)

| # | Chức năng | Trạng thái |
|---|---|---|
| 17 | **Auto-Bidding** – `maxBid`, `increment`, `PriorityQueue`, ưu tiên theo thời điểm đăng ký | ✅ |
| 18 | **Anti-Sniping** – Tự động gia hạn phiên khi có bid trong X giây cuối | ✅ |
| 19 | **Bid History Visualization** – Biểu đồ đường giá (`LineChart`) cập nhật realtime | ✅ |

### Tính năng bổ sung

| # | Tính năng |
|---|---|
| 20 | **Hệ thống Ví (Balance)** – Nạp tiền, kiểm tra số dư, tự động trừ khi thắng |
| 21 | **Watchlist** – Theo dõi / bỏ theo dõi phiên đấu giá |
| 22 | **Lịch sử đấu giá** – Xem toàn bộ lịch sử bid của phiên |
| 23 | **Quản lý Profile** – Cập nhật email, đổi mật khẩu |
| 24 | **Admin Dashboard** – Quản lý người dùng, phiên đấu giá |
| 25 | **Notification System** – Toast notifications, thông báo kết quả thắng/thua |
| 26 | **Bảo mật** – Mật khẩu được hash trước khi lưu (`SecurityUtils`) |
| 27 | **Auto-Save dữ liệu** – `AutoSaveScheduler` + `DirtyFlagTracker` định kỳ ghi `.dat` |

---

## Báo cáo & Video demo

| Nội dung | Link |
|---|---|
| Báo cáo PDF | [Xem báo cáo](https://drive.google.com/file/d/1SzLp6xhrwIa9EIf3QhNhy0gg_4h7LGpG/view?usp=sharing) |
| Video demo | [Xem video](https://link-to-video) |
