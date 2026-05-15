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
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionListController implements Initializable {

  @FXML private Label welcomeLabel;
  @FXML private FlowPane auctionGrid;
  @FXML private Button adminButton;
  @FXML private Button sellerButton;
  @FXML private Label statusBarLabel;

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

  private final Gson gson = new GsonBuilder()
          .registerTypeAdapter(java.time.LocalDateTime.class,
                  (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                          java.time.LocalDateTime.parse(json.getAsString()))
          .create();

  public void setUsername(String username) {
    this.username = username;
    welcomeLabel.setText("Xin chào, " + username + "!");
    String role = SessionManager.getInstance().getRole();
    if (adminButton != null) {
      adminButton.setVisible("ADMIN".equalsIgnoreCase(role));
      adminButton.setManaged("ADMIN".equalsIgnoreCase(role));
    }
    if (sellerButton != null) {
      sellerButton.setVisible("SELLER".equalsIgnoreCase(role));
      sellerButton.setManaged("SELLER".equalsIgnoreCase(role));
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    loadFromServer();
    registerPushListener();
    startAutoRefresh();
  }

  // ── Push Listener ─────────────────────────────────────────────────────────
  private void registerPushListener() {
    pushListener = message -> {
      String[] parts = message.split("\\|");
      String header = parts[0];

      switch (header) {
        case Protocol.NOTI_NEW_AUCTION:
          Platform.runLater(() -> {
            setStatusBar("🆕 Có phiên đấu giá mới! Đang tải lại...");
            loadFromServer();
          });
          break;

        case Protocol.RES_END_SUCCESS: {
          String auctionId = parts.length >= 2 ? parts[1] : "";
          String detail    = parts.length >= 3 ? parts[2] : "";
          // Fix: trim để tránh lỗi space trong "Winner: username"
          boolean isWin = detail.contains("Winner:" + username)
                  || detail.contains("Winner: " + username);
          if (isWin) {
            NotificationManager.getInstance().add(
                    "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId,
                    "auction", auctionId);
          } else if (!detail.contains("No winner") && !auctionId.isEmpty()) {
            NotificationManager.getInstance().add(
                    "Phiên " + auctionId + " kết thúc. Người thắng: " + extractWinner(detail) + ". Bạn không thắng.",
                    "auction", auctionId);
          }
          Platform.runLater(this::loadFromServer);
          break;
        }

        case Protocol.NOTI_OUTBID: {
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String newAmt    = parts[3];
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
          }
          break;
        }

        default:
          break;
      }
    };
    ServerConnection.getInstance().addPushListener(pushListener);
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

  // ── Filter ────────────────────────────────────────────────────────────────
  @FXML
  private void handleFilterType(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> typeButtons = List.of(btnFilterAll, btnFilterArt,
            btnFilterElec, btnFilterVehicle, btnFilterOther);
    typeButtons.forEach(b -> {
      b.getStyleClass().remove("filter-active");
      b.getStyleClass().add("filter-btn");
    });
    clicked.getStyleClass().remove("filter-btn");
    clicked.getStyleClass().add("filter-active");

    if (clicked == btnFilterArt)         activeTypeFilter = "Art";
    else if (clicked == btnFilterElec)   activeTypeFilter = "Electronics";
    else if (clicked == btnFilterVehicle) activeTypeFilter = "Vehicle";
    else if (clicked == btnFilterOther)  activeTypeFilter = "OTHER";
    else                                 activeTypeFilter = "ALL";
    applyFilter();
  }

  @FXML
  private void handleFilterPrice(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> priceButtons = List.of(btnPriceAll, btnPriceUnder5, btnPriceMid, btnPriceOver50);
    priceButtons.forEach(b -> {
      b.getStyleClass().remove("filter-active");
      b.getStyleClass().add("filter-btn");
    });
    clicked.getStyleClass().remove("filter-btn");
    clicked.getStyleClass().add("filter-active");

    if (clicked == btnPriceUnder5)      activePriceFilter = "UNDER5";
    else if (clicked == btnPriceMid)    activePriceFilter = "MID";
    else if (clicked == btnPriceOver50) activePriceFilter = "OVER50";
    else                                activePriceFilter = "ALL";
    applyFilter();
  }

  private void applyFilter() {
    List<AuctionRow> filtered = currentRows.stream()
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
      // Dùng JavaFX Image load async built-in (backgroundLoading=true)
      javafx.scene.image.Image img = new javafx.scene.image.Image(
              imgUrl, 187, 120, true, true, true); // backgroundLoading=true
      javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
      imgView.setFitWidth(187);
      imgView.setFitHeight(120);
      imgView.setPreserveRatio(true);
      imgView.setSmooth(true);
      javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(187, 120);
      clip.setArcWidth(10); clip.setArcHeight(10);
      imgView.setClip(clip);
      javafx.scene.layout.StackPane imgContainer =
              new javafx.scene.layout.StackPane(imgView);
      imgContainer.setPrefHeight(120);
      imgContainer.setStyle("-fx-background-color: #162236; -fx-background-radius: 8;");
      iconNode = imgContainer;
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
    btnDetail.setOnMouseEntered(e -> btnDetail.setStyle(
            btnDetail.getStyle().replace("linear-gradient(to right,#2563EB,#3B82F6)", "linear-gradient(to right,#1D4ED8,#2563EB)")));
    btnDetail.setOnMouseExited(e -> btnDetail.setStyle(
            btnDetail.getStyle().replace("linear-gradient(to right,#1D4ED8,#2563EB)", "linear-gradient(to right,#2563EB,#3B82F6)")));

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
                  .replace("-fx-background-color: #1E2D45;", "-fx-background-color: #0B1120;"));
        }
      });
      card.setStyle(card.getStyle()
              .replace("-fx-background-color: #0B1120;", "-fx-background-color: #162236;"));
      selectedRow = row;
    });

    return card;
  }

  private void openBidView(AuctionRow row) {
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

  @FXML
  private void startAutoRefresh() {
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

  public void handleLogout() {
    stopAutoRefresh();
    if (pushListener != null) {
      ServerConnection.getInstance().removePushListener(pushListener);
      pushListener = null;
    }
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
}