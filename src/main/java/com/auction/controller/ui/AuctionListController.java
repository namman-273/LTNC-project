package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.AlertUtil;
import com.auction.util.ui.NotificationManager;
import com.auction.network.client.ServerConnection;
import com.auction.util.core.SessionManager;
import com.auction.views.java.AdminDashboardView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.BidHistoryView;
import com.auction.views.java.BidView;
import com.auction.views.java.CreateAuctionView;
import com.auction.views.java.LoginView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.ProfileView;
import com.auction.views.java.SellerView;
import com.auction.views.java.WatchlistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AuctionListController implements Initializable {

  @FXML private Label welcomeLabel;
  @FXML private Label balanceLabel;
  @FXML private FlowPane auctionGrid;
  @FXML private Button adminButton;
  @FXML private Button sellerButton;
  @FXML private Button watchlistButton;
  @FXML private Button watchlistIconButton;
  @FXML private Label statusBarLabel;
  @FXML private TextField searchField;

  @FXML private Button btnFilterAll;
  @FXML private Button btnFilterArt;
  @FXML private Button btnFilterElec;
  @FXML private Button btnFilterVehicle;
  @FXML private Button btnFilterOther;
  @FXML private Button btnPriceAll;
  @FXML private Button btnPriceUnder5;
  @FXML private Button btnPriceMid;
  @FXML private Button btnPriceOver50;

  private String activeTypeFilter  = "ALL";
  private String activePriceFilter = "ALL";

  private String username;
  private AuctionRow selectedRow = null;
  private final List<AuctionRow> currentRows = new ArrayList<>();
  private Consumer<String> pushListener;
  private Timeline autoRefreshTimeline;
  // FIX: Track các auctionId mà user đã đặt bid để tránh nhầm "không thắng"
  private final java.util.Set<String> biddedAuctions = new java.util.HashSet<>();

  private final Gson gson = new GsonBuilder()
          .registerTypeAdapter(java.time.LocalDateTime.class,
                  (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                          java.time.LocalDateTime.parse(json.getAsString()))
          .create();

  public void setUsername(String username) {
    this.username = username;
    if (welcomeLabel != null) welcomeLabel.setText("Xin chào, " + username + "!");
    String role = SessionManager.getInstance().getRole();
    if (adminButton != null) {
      adminButton.setVisible("ADMIN".equalsIgnoreCase(role));
      adminButton.setManaged("ADMIN".equalsIgnoreCase(role));
    }
    if (sellerButton != null) {
      sellerButton.setVisible("SELLER".equalsIgnoreCase(role));
      sellerButton.setManaged("SELLER".equalsIgnoreCase(role));
    }
    // Watchlist chỉ dành cho Bidder
    boolean isBidder = !"SELLER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role);
    if (watchlistButton != null) {
      watchlistButton.setVisible(isBidder);
      watchlistButton.setManaged(isBidder);
    }
    if (watchlistIconButton != null) {
      watchlistIconButton.setVisible(isBidder);
      watchlistIconButton.setManaged(isBidder);
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    loadFromServer();
    registerPushListener();
    loadBalance();
    startAutoRefreshTimeline();
  }

  // ── Push Listener ─────────────────────────────────────────────────────────
  private void registerPushListener() {
    pushListener = message -> {
      String[] parts = message.split("\\|");
      String header = parts[0];
      switch (header) {
        case Protocol.NOTI_BID_UPDATE: {
          // BID_UPDATE|auctionId|amount|bidderName|itemType
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String amount    = parts[2];
            String bidder    = parts[3];
            try {
              double amt = Double.parseDouble(amount);
              NotificationManager.getInstance().add(
                      "🔨 Phiên " + auctionId + " có giá mới: "
                              + String.format("%,.0f", amt) + " VNĐ (bởi " + bidder + ")",
                      "auction", auctionId);
            } catch (NumberFormatException e) {
              NotificationManager.getInstance().add(
                      "🔨 Phiên " + auctionId + " có giá mới: " + amount + " VNĐ",
                      "auction", auctionId);
            }
            Platform.runLater(this::loadFromServer);
          }
          break;
        }

        case Protocol.NOTI_BALANCE_CHANGED:
          // Format: BALANCE_CHANGED|auctionId|newBalance|+amount
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String newBal    = parts[2];
            String delta     = parts[3];
            Platform.runLater(() -> {
              if (balanceLabel != null) {
                try {
                  double v = Double.parseDouble(newBal);
                  balanceLabel.setText(String.format("%,.0f VNĐ", v));
                } catch (NumberFormatException e) {
                  balanceLabel.setText(newBal + " VNĐ");
                }
              }
            });
            // Thông báo cho seller nhận tiền
            NotificationManager.getInstance().add(
                    "💰 Phiên " + auctionId + " kết thúc. Bạn nhận: "
                            + delta + " VNĐ",
                    "balance", auctionId);
          } else if (parts.length == 3) {
            String newBal = parts[2];
            Platform.runLater(() -> {
              if (balanceLabel != null) {
                try {
                  double v = Double.parseDouble(newBal);
                  balanceLabel.setText(String.format("%,.0f VNĐ", v));
                } catch (NumberFormatException e) {
                  balanceLabel.setText(newBal + " VNĐ");
                }
              }
            });
          }
          break;

        case Protocol.NOTI_NEW_AUCTION:
          Platform.runLater(() -> {
            setStatusBar("🆕 Có phiên đấu giá mới! Đang tải lại...");
            loadFromServer();
          });
          break;

        case Protocol.RES_END_SUCCESS: {
          String auctionId = parts.length >= 2 ? parts[1] : "";
          String detail    = parts.length >= 3 ? parts[2] : "";
          boolean isWin = detail.contains("Winner:" + username)
                  || detail.contains("Winner: " + username);

          if (isWin) {
            NotificationManager.getInstance().add(
                    "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId,
                    "auction", auctionId);
          } else if (!detail.contains("No winner") && !auctionId.isEmpty()) {
            String winnerName = extractWinner(detail);
            NotificationManager.getInstance().add(
                    "🔔 Phiên " + auctionId + " đã kết thúc. Người thắng: " + winnerName,
                    "auction", auctionId);
          } else if (!auctionId.isEmpty()) {
            NotificationManager.getInstance().add(
                    "🔔 Phiên " + auctionId + " đã kết thúc. Không có người thắng.",
                    "auction", auctionId);
          }
          Platform.runLater(this::loadFromServer);
          break;
        }

        case Protocol.NOTI_OUTBID: {
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String newAmt    = parts[3];
            // FIX: Đánh dấu user đã tham gia bid trong phiên này
            biddedAuctions.add(auctionId);
            NotificationManager.getInstance().add(
                    "⚠️ Bị vượt giá trong phiên " + auctionId
                            + " — Giá mới: " + newAmt + " VNĐ",
                    "auction", auctionId);
          }
          break;
        }

        case Protocol.NOTI_REFUND: {
          if (parts.length >= 3) {
            String refundAmt = parts[2];
            String auctionId = parts.length >= 2 ? parts[1] : "";
            NotificationManager.getInstance().add(
                    "💰 Hoàn tiền " + refundAmt + " VNĐ vào ví",
                    "balance", auctionId);
            // FIX: cập nhật lại số dư hiển thị sau khi được hoàn tiền
            loadBalance();
          }
          break;
        }

        default:
          break;
      }
    };
    ServerConnection.getInstance().addPushListener(pushListener);
  }

  private void removePushListener() {
    if (pushListener != null) {
      ServerConnection.getInstance().removePushListener(pushListener);
      pushListener = null;
    }
  }

  private String extractWinner(String detail) {
    if (detail == null) return "N/A";
    int idx = detail.indexOf("Winner:");
    if (idx < 0) idx = detail.indexOf("Winner: ");
    if (idx < 0) return "N/A";
    String rest = detail.substring(idx + 7).trim();
    int end = rest.indexOf("|");
    return end > 0 ? rest.substring(0, end).trim() : rest.trim();
  }

  // ── Load data ─────────────────────────────────────────────────────────────
  private void loadFromServer() {
    setStatusBar("Đang tải danh sách phiên...");
    new Thread(() -> {
      try {
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
          if (!conn.connectWithRetry()) {
            Platform.runLater(() -> {
              setStatusBar("❌ Mất kết nối server!");
              AlertUtil.showError("Mất kết nối", "Không thể kết nối server!");
            });
            return;
          }
        }
        String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
        if (response == null || response.startsWith("ERROR")) {
          Platform.runLater(() -> setStatusBar("❌ Mất kết nối server!"));
          return;
        }
        List<AuctionRow> data = new ArrayList<>();
        if (response.startsWith(Protocol.RES_LIST_SUCCESS)) {
          String json = response.substring(
                  Protocol.RES_LIST_SUCCESS.length() + Protocol.SEPARATOR.length());
          AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
          if (rows != null) {
            for (AuctionRow row : rows) {
              if (!"FINISHED".equals(row.getStatus()) && !"PAID".equals(row.getStatus())) {
                data.add(row);
              }
            }
          }
        }
        final List<AuctionRow> finalData = data;
        Platform.runLater(() -> {
          currentRows.clear();
          currentRows.addAll(finalData);
          applyFilter();
          setStatusBar(finalData.isEmpty()
                  ? "ℹ️ Chưa có phiên nào đang diễn ra."
                  : "✅ Tải xong " + finalData.size() + " phiên.");
        });
      } catch (Exception e) {
        Platform.runLater(() -> setStatusBar("❌ Lỗi tải danh sách!"));
      }
    }).start();
  }

  // ── Search ────────────────────────────────────────────────────────────────
  @FXML
  private void handleSearch(KeyEvent e) {
    if (searchField == null) return;
    String keyword = searchField.getText().trim().toLowerCase();
    if (keyword.isEmpty()) {
      applyFilter();
      return;
    }
    List<AuctionRow> filtered = currentRows.stream()
            .filter(r -> r.getItemName().toLowerCase().contains(keyword)
                    || r.getId().toLowerCase().contains(keyword))
            .collect(Collectors.toList());
    renderCards(filtered);
  }

  private static final String FILTER_ACTIVE_STYLE =
          "-fx-background-color: rgba(59,130,246,0.3); -fx-text-fill: #93C5FD;" +
                  "-fx-font-size: 11px; -fx-font-weight: bold;" +
                  "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 12;" +
                  "-fx-border-color: #3B82F6; -fx-border-radius: 8; -fx-border-width: 1;";

  private static final String FILTER_INACTIVE_STYLE =
          "-fx-background-color: transparent; -fx-text-fill: #64748B;" +
                  "-fx-font-size: 11px; -fx-background-radius: 8; -fx-cursor: hand;" +
                  "-fx-padding: 0 12; -fx-border-color: #1E3A5F;" +
                  "-fx-border-radius: 8; -fx-border-width: 1;";

  // ── Filter ────────────────────────────────────────────────────────────────
  @FXML
  private void handleFilterType(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> typeButtons = List.of(btnFilterAll, btnFilterArt,
            btnFilterElec, btnFilterVehicle, btnFilterOther);
    typeButtons.forEach(b -> b.setStyle(FILTER_INACTIVE_STYLE));
    clicked.setStyle(FILTER_ACTIVE_STYLE);

    if (clicked == btnFilterArt)          activeTypeFilter = "Art";
    else if (clicked == btnFilterElec)    activeTypeFilter = "Electronics";
    else if (clicked == btnFilterVehicle) activeTypeFilter = "Vehicle";
    else if (clicked == btnFilterOther)   activeTypeFilter = "OTHER";
    else                                  activeTypeFilter = "ALL";
    applyFilter();
  }

  @FXML
  private void handleFilterPrice(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> priceButtons = List.of(btnPriceAll, btnPriceUnder5, btnPriceMid, btnPriceOver50);
    priceButtons.forEach(b -> b.setStyle(FILTER_INACTIVE_STYLE));
    clicked.setStyle(FILTER_ACTIVE_STYLE);

    if (clicked == btnPriceUnder5)      activePriceFilter = "UNDER5";
    else if (clicked == btnPriceMid)    activePriceFilter = "MID";
    else if (clicked == btnPriceOver50) activePriceFilter = "OVER50";
    else                                activePriceFilter = "ALL";
    applyFilter();
  }

  private void applyFilter() {
    String keyword = (searchField != null && searchField.getText() != null)
            ? searchField.getText().trim().toLowerCase() : "";

    List<AuctionRow> filtered = currentRows.stream()
            .filter(r -> keyword.isEmpty()
                    || r.getItemName().toLowerCase().contains(keyword)
                    || r.getId().toLowerCase().contains(keyword))
            .filter(r -> {
              String type = r.getItemType() != null ? r.getItemType() : "";
              return switch (activeTypeFilter) {
                case "Art"         -> "Art".equals(type);
                case "Electronics" -> "Electronics".equals(type);
                case "Vehicle"     -> "Vehicle".equals(type);
                case "OTHER"       -> !List.of("Art", "Electronics", "Vehicle").contains(type);
                default            -> true;
              };
            })
            .filter(r -> switch (activePriceFilter) {
              case "UNDER5"  -> r.getCurrentPrice() < 5_000_000;
              case "MID"     -> r.getCurrentPrice() >= 5_000_000 && r.getCurrentPrice() <= 50_000_000;
              case "OVER50"  -> r.getCurrentPrice() > 50_000_000;
              default        -> true;
            })
            .collect(Collectors.toList());
    renderCards(filtered);
  }

  // ── Card rendering ────────────────────────────────────────────────────────
  private void renderCards(List<AuctionRow> rows) {
    auctionGrid.getChildren().clear();
    selectedRow = null;
    if (rows.isEmpty()) {
      Label empty = new Label("Không có phiên nào phù hợp.");
      empty.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px; -fx-padding: 40;");
      auctionGrid.getChildren().add(empty);
      return;
    }
    for (AuctionRow row : rows) {
      auctionGrid.getChildren().add(buildCard(row));
    }
  }

  private VBox buildCard(AuctionRow row) {
    String statusColor = switch (row.getStatus()) {
      case "RUNNING" -> "#E65100";
      case "OPEN"    -> "#2E7D32";
      default        -> "#888888";
    };
    Label badge = new Label("RUNNING".equals(row.getStatus()) ? "🔴 LIVE" : "⬤ " + row.getStatus());
    badge.setStyle(
            "-fx-background-color: " + statusColor + "22;" +
                    "-fx-text-fill: " + statusColor + ";" +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 6; -fx-padding: 3 8;");

    String typeIcon = switch (row.getItemType() != null ? row.getItemType() : "") {
      case "Art"         -> "🎨";
      case "Electronics" -> "⚡";
      case "Vehicle"     -> "🚗";
      default            -> "🏷";
    };

    javafx.scene.Node iconNode;
    String imgUrl = row.getImageUrl();
    if (imgUrl != null && !imgUrl.isEmpty()) {
      // Tạo placeholder trước, load ảnh nền sau
      javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
      imgView.setFitWidth(187);
      imgView.setFitHeight(120);
      imgView.setPreserveRatio(true);
      imgView.setSmooth(true);
      javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(187, 120);
      clip.setArcWidth(10);
      clip.setArcHeight(10);
      imgView.setClip(clip);
      Label placeholderIcon = new Label(typeIcon);
      placeholderIcon.setStyle("-fx-font-size: 40px;");
      javafx.scene.layout.StackPane imgContainer =
              new javafx.scene.layout.StackPane(placeholderIcon, imgView);
      imgContainer.setPrefWidth(187);
      imgContainer.setPrefHeight(120);
      imgContainer.setStyle("-fx-background-color: #162236; -fx-background-radius: 8;");
      iconNode = imgContainer;
      // Load ảnh bằng background thread — dùng requestedWidth/Height để JavaFX scale ngay
      // lúc decode, tránh OOM khi ảnh base64 gốc quá lớn
      final String finalImgUrl = imgUrl;
      new Thread(() -> {
        try {
          javafx.scene.image.Image img;
          // Kích thước tối đa để render — scale khi decode, không giữ toàn bộ buffer
          final double REQ_W = 187, REQ_H = 120;
          if (finalImgUrl.startsWith("data:image")) {
            String base64 = finalImgUrl.substring(finalImgUrl.indexOf(",") + 1);
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            img = new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(bytes),
                    REQ_W, REQ_H, true, true);
            bytes = null; // GC sớm
          } else {
            java.net.URL url = new java.net.URL(finalImgUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "image/*,*/*");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.connect();
            try (java.io.InputStream is = conn.getInputStream()) {
              byte[] bytes = is.readAllBytes();
              img = new javafx.scene.image.Image(
                      new java.io.ByteArrayInputStream(bytes),
                      REQ_W, REQ_H, true, true);
            } finally {
              conn.disconnect();
            }
          }
          if (!img.isError()) {
            final javafx.scene.image.Image finalImg = img;
            javafx.application.Platform.runLater(() -> {
              imgView.setImage(finalImg);
              placeholderIcon.setVisible(false);
            });
          }
        } catch (Exception e) {
          System.err.println("[AuctionList] Không load được ảnh: " + e.getMessage());
        }
      }, "card-img-load").start();
    } else {
      Label icon = new Label(typeIcon);
      icon.setStyle("-fx-font-size: 46px; -fx-padding: 8 0;");
      icon.setMinWidth(187);
      icon.setAlignment(javafx.geometry.Pos.CENTER);
      iconNode = icon;
    }

    Label name = new Label(row.getItemName());
    name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #E2E8F0; -fx-wrap-text: true;");
    name.setMaxWidth(185);

    Label priceLabel = new Label("Giá hiện tại");
    priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569;");
    Label price = new Label(row.getCurrentPriceFormatted());
    price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #60A5FA;");

    Button btnDetail = new Button("👁 Xem chi tiết");
    btnDetail.setPrefWidth(185);
    btnDetail.setStyle(
            "-fx-background-color: #1565C0; -fx-text-fill: white;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 0;");
    btnDetail.setOnAction(e -> openBidView(row));

    VBox card = new VBox(8, badge, iconNode, name, priceLabel, price, btnDetail);
    card.setPrefWidth(215);
    card.setMinHeight(265);
    card.setStyle(
            "-fx-background-color: #0B1120;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #1E3A5F; -fx-border-radius: 14; -fx-border-width: 1;" +
                    "-fx-padding: 14;" +
                    "-fx-cursor: hand;");

    card.setOnMouseClicked(e -> {
      auctionGrid.getChildren().forEach(n -> {
        if (n instanceof VBox v) {
          v.setStyle(v.getStyle()
                  .replace("-fx-background-color: #162236;", "-fx-background-color: #0B1120;"));
        }
      });
      card.setStyle(card.getStyle()
              .replace("-fx-background-color: #0B1120;", "-fx-background-color: #162236;"));
      selectedRow = row;
    });

    return card;
  }

  private void openBidView(AuctionRow row) {
    stopAutoRefresh();
    removePushListener();
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BidView(stage, row.getId(), row.getItemName(),
            String.valueOf(row.getCurrentPrice()), row.getStatus(),
            username, row.getEndTime(),
            row.getImageUrl(), row.getDescription(),
            row.getItemType(), row.getStartingPrice(), row.getSellerId()).show();
  }

  // ── Watchlist ─────────────────────────────────────────────────────────────
  @FXML
  public void handleWatch() {
    if (selectedRow == null) {
      setStatusBar("⚠️ Vui lòng click vào một phiên trước!");
      return;
    }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_WATCH + Protocol.SEPARATOR + selectedRow.getId());
      Platform.runLater(() -> {
        if (response != null && response.startsWith(Protocol.RES_WATCH_SUCCESS)) {
          setStatusBar("✅ Đã theo dõi phiên!");
          AlertUtil.showSuccess("Theo dõi thành công",
                  "✅ Đang theo dõi: " + selectedRow.getItemName());
        } else {
          setStatusBar("❌ Theo dõi thất bại!");
        }
      });
    }).start();
  }

  @FXML
  public void handleUnwatch() {
    if (selectedRow == null) {
      setStatusBar("⚠️ Vui lòng click vào một phiên trước!");
      return;
    }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selectedRow.getId());
      Platform.runLater(() -> {
        if (response != null && response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
          setStatusBar("✅ Đã bỏ theo dõi!");
        } else {
          setStatusBar("❌ Bỏ theo dõi thất bại!");
        }
      });
    }).start();
  }

  @FXML
  public void handleGetWatchlist() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new WatchlistView(stage, username).show();
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  private void setStatusBar(String msg) {
    if (statusBarLabel != null) statusBarLabel.setText(msg);
  }

  @FXML
  public void handleRefresh() { loadFromServer(); }

  public void refreshList() { loadFromServer(); }

  private void startAutoRefreshTimeline() {
    autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(8), e -> loadFromServer()));
    autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    autoRefreshTimeline.play();
  }

  private void stopAutoRefresh() {
    if (autoRefreshTimeline != null) {
      autoRefreshTimeline.stop();
      autoRefreshTimeline = null;
    }
  }

  @FXML
  public void handleLogout() {
    stopAutoRefresh();
    removePushListener();
    ServerConnection.getInstance().disconnect();
    SessionManager.getInstance().clear();
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new LoginView(stage).show();
  }

  @FXML
  public void handleCreateAuction() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new CreateAuctionView(stage, username).show();
  }

  @FXML
  public void handleAdminDashboard() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new AdminDashboardView(stage, username).show();
  }

  @FXML
  public void handleSellerDashboard() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new SellerView(stage, username).show();
  }

  @FXML
  private void loadBalance() {
    if (balanceLabel == null) return;
    new Thread(() -> {
      String res = com.auction.network.client.ServerConnection.getInstance()
              .sendAndReceive(com.auction.network.protocol.Protocol.CMD_GET_BALANCE);
      Platform.runLater(() -> {
        if (res != null && res.startsWith(com.auction.network.protocol.Protocol.RES_BALANCE_INFO)) {
          String[] p = res.split("\\|", -1);
          String amt = p.length >= 2 ? p[1] : "---";
          try {
            double v = Double.parseDouble(amt);
            if (balanceLabel != null) balanceLabel.setText(String.format("%,.0f VNĐ", v));
          } catch (NumberFormatException e) {
            if (balanceLabel != null) balanceLabel.setText(amt + " VNĐ");
          }
        } else {
          if (balanceLabel != null) balanceLabel.setText("---");
        }
      });
    }, "auctionlist-balance-thread").start();
  }

  public void handleBalance() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BalanceView(stage, username).show();
  }

  @FXML
  public void handleNotification() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new NotificationView(stage, username).show();
  }

  @FXML
  public void handleBidHistory() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BidHistoryView(stage, username).show();
  }

  @FXML
  public void handleProfile() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new ProfileView(stage, username).show();
  }
  @FXML
  public void handleToggleSidebar() {
    // Sidebar toggle — có thể mở rộng sau
  }
}