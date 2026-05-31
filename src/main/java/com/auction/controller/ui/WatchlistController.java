package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.ToastManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.BidHistoryView;
import com.auction.views.java.BidView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.ProfileView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *.
     */
public class WatchlistController extends BaseController implements Initializable {

  @FXML
  private TableView<AuctionRow> watchlistTable;
  @FXML
  private TableColumn<AuctionRow, String> idCol;
  @FXML
  private TableColumn<AuctionRow, String> nameCol;
  @FXML
  private TableColumn<AuctionRow, String> priceCol;
  @FXML
  private TableColumn<AuctionRow, String> statusCol;
  @FXML
  private Label messageLabel;
  @FXML
  private VBox watchlistCards;
  @FXML
  private Label totalCountLabel;
  @FXML
  private Label openCountLabel;
  @FXML
  private Label finishedCountLabel;
  @FXML
  private Label balanceLabel;

  private String username;
  private ObservableList<AuctionRow> currentData = FXCollections.observableArrayList();
  private Timeline autoRefreshTimeline;

  // Track watched IDs (thread-safe) để push listener check nhanh
  private final java.util.Set<String> watchedAuctionIds = java.util.Collections
      .synchronizedSet(new java.util.HashSet<>());
  // Track số lần gia hạn per auctionId
  private final java.util.Map<String, Integer> extensionCountMap = 
      new java.util.concurrent.ConcurrentHashMap<>();

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(java.time.LocalDateTime.class,
          (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) 
            -> java.time.LocalDateTime
              .parse(json.getAsString()))
      .create();

  /**
 *.
     */
  public void setUsername(String username) {
    this.username = username;
    initToastManager(watchlistCards); // BaseController — loại bỏ bản copy
    loadWatchlist();
    loadBalance(balanceLabel); // BaseController — loại bỏ bản copy
    startAutoRefresh();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    registerPushListener(this::handlePushMessage); // BaseController
    if (idCol != null) {
      idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    }
    if (nameCol != null) {
      nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    }
    if (priceCol != null) {
      priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));
    }
    if (statusCol != null) {
      statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }
  }

  // ── Push ──────────────────────────────────────────────────────────────────
  private void handlePushMessage(String message) {
    String[] parts = message.split("\\|");
    if (parts.length == 0) {
      return;
    }

    switch (parts[0]) {

      case Protocol.NOTI_BALANCE_CHANGED:
        if (parts.length >= 3) {
          Platform.runLater(() 
              -> updateBalanceLabelFromPush(balanceLabel, parts[2])); // BaseController
        }
        break;

      case Protocol.NOTI_BID_UPDATE:
        if (parts.length >= 4) {
          String auctionId = parts[1];
          String amount = parts[2];
          String bidder = parts[3];
          if (watchedAuctionIds.contains(auctionId)) {
            try {
              double amt = Double.parseDouble(amount);
              String msg = "🔨 Giá mới tại phiên " + auctionId + ": "
                  + AuctionUtils.formatPrice(amt) + " (bởi " + bidder + ")"; // AuctionUtils
              NotificationManager.getInstance().add(msg, "auction", auctionId);
              Platform.runLater(() -> {
                ToastManager.show(ToastManager.Type.INFO,
                    "Giá mới: " + AuctionUtils.formatPrice(amt) + " – " + bidder);
                loadWatchlist();
              });
            } catch (NumberFormatException e) {
              NotificationManager.getInstance().add(
                  "🔨 Giá mới tại phiên " + auctionId + ": " 
                  + amount + " VNĐ", "auction", auctionId);
              Platform.runLater(() 
                  -> ToastManager.show(ToastManager.Type.INFO, "Giá mới: " + amount + " VNĐ"));
            }
          }
        }
        break;

      case Protocol.NOTI_SNIPING_UPDATE:
        if (parts.length >= 4) {
          String auctionId = parts[1];
          String count = parts[3];
          if (watchedAuctionIds.contains(auctionId)) {
            NotificationManager.getInstance().add(
                "⏱ Phiên " + auctionId + " được gia hạn lần " + count + " (+2 phút)",
                "auction", auctionId);
            try {
              extensionCountMap.put(auctionId, Integer.parseInt(count));
            } catch (NumberFormatException ignored) {
              ignored.printStackTrace();
            }
            Platform.runLater(() -> {
              ToastManager.show(ToastManager.Type.WARNING,
                  "⏱ Phiên " + auctionId + " gia hạn lần " + count + " (+2 phút)");
              updateCards(currentData);
            });
          }
        }
        break;

      // RES_END_SUCCESS là direct response — không xử lý ở push listener để tránh
      // popup 2 lần
      // case Protocol.RES_END_SUCCESS: → ĐÃ BỎ CỐ TÌNH

      case Protocol.NOTI_AUCTION_CANCELLED:
        if (parts.length >= 3) {
          String cancelledId = parts[1];
          String reason = parts[2];
          if (watchedAuctionIds.contains(cancelledId)) {
            NotificationManager.getInstance().add(
                "❌ Phiên " + cancelledId + ": " + reason + ". Tiền đã được hoàn.",
                "auction", cancelledId);
            Platform.runLater(() -> {
              watchedAuctionIds.remove(cancelledId);
              currentData.removeIf(r -> r.getId().equals(cancelledId));
              updateCards(currentData);
              ToastManager.show(ToastManager.Type.WARNING,
                  "❌ Phiên " + cancelledId + ": " + reason);
            });
          }
        }
        break;

      default:
        break;
    }
  }

  // ── Load data ─────────────────────────────────────────────────────────────
  private void loadWatchlist() {
    showMessage("Đang tải watchlist...", "#6B7280");
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_GET_WATCHLIST);
      System.out.println("Watchlist response: " + response);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("❌ Mất kết nối server!", "#DC2626");
          return;
        }
        ObservableList<AuctionRow> data = FXCollections.observableArrayList();
        if (response.startsWith(Protocol.RES_WATCHLIST)) {
          String json = response.substring(
              Protocol.RES_WATCHLIST.length() + Protocol.SEPARATOR.length());
          AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
          if (rows != null) {
            data.addAll(rows);
          }
        } else {
          String[] parts = response.split("\\" + Protocol.SEPARATOR);
          showMessage("❌ " + (parts.length > 1 ? parts[1] : "Lỗi tải watchlist!"), "#DC2626");
          return;
        }
        currentData.setAll(data);
        watchedAuctionIds.clear();
        for (AuctionRow row : data) {
          watchedAuctionIds.add(row.getId());
        }
        if (watchlistTable != null) {
          watchlistTable.setItems(data);
        }
        updateCards(data);
        showMessage(data.isEmpty()
            ? "Chưa có phiên nào trong watchlist."
            : "✅ " + data.size() + " phiên đang theo dõi.",
            data.isEmpty() ? "#6B7280" : "#059669");
      });
    }).start();
  }

  private void updateCards(ObservableList<AuctionRow> data) {
    if (watchlistCards == null) {
      return;
    }
    watchlistCards.getChildren().clear();

    long open = data.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
    long finished = data.stream()
        .filter(r -> AuctionUtils.isFinishedStatus(r.getStatus())).count(); // AuctionUtils
    if (totalCountLabel != null) {
      totalCountLabel.setText(String.valueOf(data.size()));
    }
    if (openCountLabel != null) {
      openCountLabel.setText(String.valueOf(open));
    }
    if (finishedCountLabel != null) {
      finishedCountLabel.setText(String.valueOf(finished));
    }

    if (data.isEmpty()) {
      VBox empty = new VBox();
      empty.setAlignment(Pos.CENTER);
      empty.setStyle("-fx-padding: 60 0;");
      Label emptyLabel = new Label("⭐ Chưa có phiên nào trong watchlist");
      emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF;");
      empty.getChildren().add(emptyLabel);
      watchlistCards.getChildren().add(empty);
      return;
    }
    for (AuctionRow row : data) {
      watchlistCards.getChildren().add(buildCard(row));
    }
  }

  private HBox buildCard(AuctionRow row) {
    boolean isOpen = "OPEN".equals(row.getStatus()) || "RUNNING".equals(row.getStatus());
    String initials = row.getItemName().length() >= 2
        ? row.getItemName().substring(0, 2).toUpperCase()
        : row.getItemName().toUpperCase();
    Label avatar = new Label(initials);
    avatar.setStyle("-fx-background-color: " + (isOpen ? "#DBEAFE" : "#F3F4F6") + ";"
        + "-fx-text-fill: " + (isOpen ? "#1565C0" : "#6B7280") + ";"
        + "-fx-font-size: 13px; -fx-font-weight: bold;"
        + "-fx-background-radius: 8; -fx-padding: 8 10;"
        + "-fx-min-width: 44; -fx-min-height: 44; -fx-alignment: CENTER;");

    Label name = new Label(row.getItemName());
    name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
    Label price = new Label(row.getCurrentPriceFormatted());
    price.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
    VBox info = new VBox(3, name, price);
    HBox.setHgrow(info, Priority.ALWAYS);

    int extCount = extensionCountMap.getOrDefault(row.getId(), 0);
    if (extCount > 0) {
      Label extBadge = new Label("⏱ Gia hạn " + extCount + "/3");
      extBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
          + "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;"
          + "-fx-background-radius: 20; -fx-padding: 2 8;");
      info.getChildren().add(extBadge);
    }

    String badgeBg; 
    String badgeFg;
    switch (row.getStatus()) {
      case "OPEN" -> {
        badgeBg = "#D1FAE5";
        badgeFg = "#059669";
      }
      case "RUNNING" -> {
        badgeBg = "#FEF3C7";
        badgeFg = "#D97706";
      }
      case "FINISHED" -> {
        badgeBg = "#F3F4F6";
        badgeFg = "#6B7280";
      }
      default -> {
        badgeBg = "#F3F4F6";
        badgeFg = "#6B7280";
      }
    }
    Label status = new Label(row.getStatus());
    status.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
        + "-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + ";"
        + "-fx-background-radius: 20; -fx-padding: 3 10;");

    Button btnSelect = new Button("Chọn");
    btnSelect.setStyle("-fx-background-color: transparent; -fx-text-fill: #1565C0;"
        + "-fx-font-size: 11px; -fx-cursor: hand;"
        + "-fx-border-color: #DBEAFE; -fx-border-radius: 6; -fx-border-width: 1;"
        + "-fx-background-radius: 6; -fx-padding: 4 10;");
    btnSelect.setOnAction(e -> {
      if (watchlistTable != null) {
        watchlistTable.getSelectionModel().select(row);
      }
      highlightSelected(row);
    });

    HBox card = new HBox(12, avatar, info, status, btnSelect);
    card.setAlignment(Pos.CENTER_LEFT);
    card.setPadding(new Insets(12, 14, 12, 14));
    card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
        + "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;"
        + "-fx-cursor: hand;");
    VBox.setMargin(card, new Insets(0, 0, 8, 0));
    card.setOnMouseClicked(e -> {
      if (watchlistTable != null) {
        watchlistTable.getSelectionModel().select(row);
      }
      highlightSelected(row);
    });
    return card;
  }

  private void highlightSelected(AuctionRow selected) {
    if (watchlistCards == null) {
      return;
    }
    watchlistCards.getChildren().forEach(node -> {
      if (node instanceof HBox card) {
        card.setStyle(card.getStyle().replace(
            "-fx-border-color: #1565C0;", "-fx-border-color: #E5E7EB;"));
      }
    });
    int idx = currentData.indexOf(selected);
    if (idx >= 0 && idx < watchlistCards.getChildren().size()) {
      HBox card = (HBox) watchlistCards.getChildren().get(idx);
      card.setStyle(card.getStyle().replace(
          "-fx-border-color: #E5E7EB;", "-fx-border-color: #1565C0;"));
    }
  }

  // ── Actions ───────────────────────────────────────────────────────────────
  @FXML
  public void handleRefresh() {
    loadWatchlist();
  }

  /**
 *.
     */
  @FXML
  public void handleUnwatch() {
    AuctionRow selected = watchlistTable != null
        ? watchlistTable.getSelectionModel().getSelectedItem()
        : null;
    if (selected == null) {
      showMessage("⚠️ Vui lòng chọn một phiên trước!", "#D97706");
      return;
    }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selected.getId());
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("❌ Mất kết nối!", "#DC2626");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
          showMessage("✅ " + (parts.length > 1 ? parts[1] : "Đã bỏ theo dõi!"), "#059669");
          loadWatchlist();
        } else {
          showMessage("❌ " + (parts.length > 1 ? parts[1] : "Thất bại!"), "#DC2626");
        }
      });
    }).start();
  }

  /**
 *.
     */
  @FXML
  public void handleViewDetail() {
    AuctionRow selected = watchlistTable != null
        ? watchlistTable.getSelectionModel().getSelectedItem()
        : null;
    if (selected == null) {
      showMessage("⚠️ Vui lòng chọn một phiên trước!", "#D97706");
      return;
    }
    leaveScreen();
    Stage stage = getStage();
    if (stage != null) {
      new BidView(stage, selected.getId(), selected.getItemName(),
          String.valueOf(selected.getCurrentPrice()), selected.getStatus(),
          username, selected.getEndTime()).show();
    }
  }

  private void startAutoRefresh() {
    autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> loadWatchlist()));
    autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    autoRefreshTimeline.play();
  }

  private void stopAutoRefresh() {
    if (autoRefreshTimeline != null) {
      autoRefreshTimeline.stop();
      autoRefreshTimeline = null;
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  private void leaveScreen() {
    removePushListener(); // BaseController
    stopAutoRefresh();
  }

  /**
 *.
     */
  @FXML
  public void handleProfile() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new ProfileView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleBidHistory() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new BidHistoryView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleBalance() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new BalanceView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleNotification() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new NotificationView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleHome() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new AuctionListView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleBack() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new AuctionListView(s, username).show();
    }
  }

  /**
 *.
     */
  public void handleGoBalance() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new BalanceView(s, username).show();
    }
  }

  /**
 *.
     */
  @FXML
  public void handleGoNotification() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new NotificationView(s, username).show();
    }
  }

  private Stage getStage() {
    try {
      if (watchlistCards != null) {
        return (Stage) watchlistCards.getScene().getWindow();
      }
      if (watchlistTable != null) {
        return (Stage) watchlistTable.getScene().getWindow();
      }
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
    return null;
  }

  private void showMessage(String msg, String color) {
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }
}