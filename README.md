# 🏛️ Auction System

<!--
    QUAN TRỌNG: Thay TEN_NGUOI_DUNG/TEN_REPO bằng đường dẫn GitHub thật.
    Ví dụ: nhom5-ltnc/AuctionSystem
    Badge sẽ tự động hiển thị xanh/đỏ theo kết quả CI mới nhất.
-->
![CI](https://github.com/TEN_NGUOI_DUNG/TEN_REPO/actions/workflows/maven.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-007396?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-orange)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven)

Hệ thống đấu giá trực tuyến xây dựng theo kiến trúc **Client–Server**,
sử dụng **Java 17**, **JavaFX** và mô hình **MVC**.

Bài tập lớn — Lập trình nâng cao, Học kỳ II 2025–2026.

---

## Mục lục

- [Tính năng](#tính-năng)
- [Kiến trúc](#kiến-trúc)
- [Yêu cầu](#yêu-cầu)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [Unit Test & Coverage](#unit-test--coverage)
- [CI/CD](#cicd)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Thành viên nhóm](#thành-viên-nhóm)

---

## Tính năng

### Bắt buộc
| Chức năng | Trạng thái |
|-----------|-----------|
| Đăng ký / Đăng nhập (Bidder, Seller, Admin) | ✅ |
| Quản lý sản phẩm đấu giá (CRUD) | ✅ |
| Đặt giá realtime, kiểm tra hợp lệ | ✅ |
| Tự động đóng phiên, xác định người thắng | ✅ |
| Xử lý ngoại lệ (`InvalidBidException`, `AuctionClosedException`, ...) | ✅ |
| Giao diện JavaFX + FXML | ✅ |
| Kiến trúc Client–Server qua Socket | ✅ |
| Lưu trữ dữ liệu (Serialization) | ✅ |

### Nâng cao
| Chức năng | Trạng thái |
|-----------|-----------|
| Auto-Bidding (maxBid, increment, PriorityQueue) | ✅ |
| Anti-sniping (gia hạn khi bid trong 60s cuối) | ✅ |
| Bid History Visualization (LineChart realtime) | ✅ |

---

## Kiến trúc

```
┌──────────────────────────────────┐     Socket      ┌─────────────────────────────────┐
│         CLIENT (JavaFX)          │   Port 9999      │            SERVER               │
│                                  │◄────────────────►│                                 │
│  View (FXML)                     │                  │  AuctionServer                  │
│    └─ Controller                 │   TEXT PROTOCOL  │    └─ ClientHandler (Thread)    │
│         └─ ServerConnection      │  LOGIN|user|pass │         └─ AuctionService       │
│                                  │  BID|id|amount   │              └─ UserManager     │
│  Observer: nhận UPDATE realtime  │  LIST_AUCTIONS   │                   └─ DataMgr    │
└──────────────────────────────────┘                  └─────────────────────────────────┘
```

### Design Patterns
- **Singleton**: `AuctionService`, `UserManager`, `DataManager`, `ServerConnection`
- **Factory Method**: `ItemFactory` → `ElectronicsFactory`, `ArtFactory`, `VehicleFactory`
- **Observer**: `ClientHandler` implements `Observer` → nhận realtime bid update

### Cây kế thừa OOP
```
Entity (abstract)
├── User (abstract) → Bidder, Seller, Admin
└── Item (abstract) → Electronics, Art, Vehicle
```

---

## Yêu cầu

| Công cụ | Phiên bản |
|---------|-----------|
| JDK     | 17        |
| Maven   | 3.8+      |

> JavaFX 17.0.8 và tất cả dependencies được Maven tự tải về, không cần cài thủ công.

---

## Cài đặt và chạy

### 1. Clone repository

```bash
git clone https://github.com/TEN_NGUOI_DUNG/TEN_REPO.git
cd TEN_REPO
```

### 2. Build toàn bộ dự án

```bash
mvn clean verify
```

Lệnh này thực hiện theo thứ tự: **Compile → JUnit Test → JaCoCo Report → Checkstyle → JaCoCo Check (≥60%)**.
Build thành công khi tất cả bước đều pass.

### 3. Khởi động Server

```bash
mvn exec:java -Dexec.mainClass="com.auction.network.AuctionServer"
```

Hoặc chạy `AuctionServer.java → main()` trong IDE. Server lắng nghe tại cổng **9999**:

```
SERVER: Đang chạy trên cổng 9999
```

### 4. Khởi động Client (mở terminal mới)

```bash
mvn javafx:run
```

Hoặc chạy `MainApp.java → main()` trong IDE.

> **Chạy nhiều client đồng thời**: mở thêm terminal và lặp lại bước 4.
> Mỗi client nhận thông báo realtime khi client khác đặt giá.

### Tài khoản mặc định

| Username | Password   | Vai trò |
|----------|------------|---------|
| `admin`  | `admin123` | Admin   |

Tài khoản Bidder và Seller: tạo qua màn hình **Đăng ký** trong ứng dụng.

---

## Unit Test & Coverage

```bash
# Chạy tất cả test
mvn test

# Xem báo cáo coverage chi tiết (sau khi chạy test)
# Mở file này trong trình duyệt:
target/site/jacoco/index.html
```

**Coverage yêu cầu: ≥ 60% line coverage** (enforce tự động bởi JaCoCo, build fail nếu không đạt).

---

## CI/CD

Pipeline **GitHub Actions** tự động chạy khi push lên `master` hoặc `dev`:

```
Push / Pull Request
        │
        ▼
 ┌─────────────┐     ┌──────────────┐     ┌────────────────┐     ┌──────────────────┐
 │   Compile   │────►│  JUnit Test  │────►│ JaCoCo Report  │────►│    Checkstyle    │
 │  Java 17    │     │  Surefire    │     │  HTML Report   │     │  checkstyle.xml  │
 └─────────────┘     └──────────────┘     └────────────────┘     └──────────────────┘
                                                                           │
                                                                           ▼
                                                                  ┌──────────────────┐
                                                                  │  JaCoCo Check    │
                                                                  │  coverage ≥ 60%  │
                                                                  └──────────────────┘
```

Xem chi tiết tại tab **Actions** trên GitHub.
Báo cáo JaCoCo và Surefire được lưu trong **Artifacts** của mỗi run.

---

## Cấu trúc dự án

```
AuctionSystem/
├── .github/
│   └── workflows/
│       └── maven.yml               # CI/CD pipeline
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── MainApp.java
│   │   │   ├── controllers/        # JavaFX Controllers (MVC)
│   │   │   ├── model/              # Entity, User, Item, Auction, ...
│   │   │   ├── factory/            # Factory Method pattern
│   │   │   ├── exception/          # Custom exceptions
│   │   │   ├── network/            # AuctionServer, ClientHandler
│   │   │   ├── service/            # AuctionService, UserManager
│   │   │   └── util/               # DataManager, ServerConnection
│   │   └── resources/
│   │       └── com/auction/views/  # FXML files
│   └── test/
│       └── java/com/auction/
│           └── service/
│               └── AuctionServiceTest.java
├── checkstyle.xml                  # Coding convention rules
├── pom.xml                         # Maven build config
└── README.md
```

---

## Thành viên nhóm

| Họ tên             |  MSSV    | Phụ trách                          |
|--------------      |--------  |------------------------------------|
| Nguyễn Đăng Nam    | 25021902 | Network, Socket, DataManager       |
| Phạm Trung Hiếu    | 25021766 | Model, Factory, Observer, CI/CD    |
| Nguyễn Đăng Dương  | 25021697 | GUI JavaFX, Controllers, FXML      |
| Phan Như Minh Quân | 25021963 | Server, AuctionService, Unit Test  |
