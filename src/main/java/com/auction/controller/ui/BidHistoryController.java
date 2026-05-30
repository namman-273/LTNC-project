package com.auction.controller.ui;

import com.auction.model.dto.BidHistoryEntry;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.SessionManager;
import com.auction.util.ui.NotificationManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.ProfileView;
import com.auction.views.java.SellerView;
import com.auction.views.java.WatchlistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * bidhistorycontroller.
 */
public class BidHistoryController extends BaseController implements Initializable {

  @FXML
  private ListView<BidHistoryEntry> historyList;
  @FXML
  private Label totalLabel;
  @FXML
  private Label winLabel;
  @FXML
  private Label loseLabel;
  @FXML
  private Label rateLabel;
  @FXML
  private Label subtitleLabel;
  @FXML
  private Label balanceLabel;
  @FXML
  private Button tabAll;
  @FXML
  private Button tabWin;
  @FXML
  private Button tabLose;
  @FXML
  private Button sellerBtnHistory;
  @FXML
  private Button watchlistBtnHistory;

  private String username;
  private String activeTab = "all";
  private List<BidHistoryEntry> allEntries = new ArrayList<>();

  private static final String TAB_ACTIVE = "-fx-background-color: #111827; -fx-text-fill: white; "
      + "-fx-font-weight: bold; -fx-background-radius: 20; "
      + "-fx-padding: 6 18; -fx-cursor: hand; -fx-font-size: 12px; "
      + "-fx-border-color: transparent;";
  private static final String TAB_INACTIVE = "-fx-background-color: transparent;"
      + " -fx-text-fill: #6B7280; -fx-background-radius: 20; -fx-padding: 6 18;"
      + " -fx-cursor: hand; -fx-font-size: 12px;"
      + " -fx-border-color: #E5E7EB; -fx-border-radius: 20; -fx-border-width: 1;";

  private final Gson gson = new GsonBuilder().create();

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    historyList.setCellFactory(lv -> new HistoryCell());
  }

  /**
   * set users name.
   */
  public void setUsername(String u) {
    initToastManager(historyList); // BaseController — loại bỏ bản copy
    this.username = u;

    String role = SessionManager.getInstance().getRole();
    boolean isSeller = "SELLER".equalsIgnoreCase(role);
    if (sellerBtnHistory != null) {
      sellerBtnHistory.setVisible(isSeller);
      sellerBtnHistory.setManaged(isSeller);
    }
    if (watchlistBtnHistory != null) {
      watchlistBtnHistory.setVisible(!isSeller);
      watchlistBtnHistory.setManaged(!isSeller);
    }

    loadFromServer();
    registerPushListener(this::handlePushMessage);
    loadBalance(balanceLabel);
  }

  // ── Push ──────────────────────────────────────────────────────────────────
  private void handlePushMessage(String message) {
    String[] parts = message.split("\\|");
    String header = parts[0];

    if (Protocol.NOTI_BALANCE_CHANGED.equals(header)) {
      if (parts.length >= 2) {
        Platform.runLater(() -> updateBalanceLabelFromPush(balanceLabel, parts[1]));
      }

    } else if (Protocol.NOTI_SNIPING_UPDATE.equals(header)) {
      if (parts.length >= 4) {
        String auctionId = parts[1];
        String count = parts[3];
        NotificationManager.getInstance().add(
            "⏱ Phiên " + auctionId + " được gia hạn lần " + count + " (+2 phút)",
            "auction", auctionId);
      }

    } else if (Protocol.RES_END_SUCCESS.equals(header)) {
      String auctionId = parts.length >= 2 ? parts[1] : "";
      String detail = parts.length >= 3 ? parts[2] : "";
      boolean isWin = detail.contains("Winner:" + username)
          || detail.contains("Winner: " + username);
      if (!isWin && !detail.contains("No winner") && !auctionId.isEmpty()) {
        NotificationManager.getInstance().add(
            "⚠️ Phiên " + auctionId + " đã kết thúc. Bạn không thắng lần này.",
            "auction", auctionId);
      }
      new Thread(() -> {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException ignored) {
          ignored.printStackTrace();
        }
        loadFromServer();
      }).start();
    }
  }

  // ── Load ─────────────────────────────────────────────────────────────────
  private void loadFromServer() {
    new Thread(() -> {
      try {
        String res = ServerConnection.getInstance()
            .sendAndReceive(Protocol.CMD_GET_BID_HISTORY);
        if (res != null && res.startsWith(Protocol.RES_BID_HISTORY)) {
          String json = res.substring(
              Protocol.RES_BID_HISTORY.length() + Protocol.SEPARATOR.length());
          Type listType = new TypeToken<List<BidHistoryEntry>>() {
          }.getType();
          List<BidHistoryEntry> entries = gson.fromJson(json, listType);
          if (entries != null) {
            java.util.Collections.reverse(entries);
            allEntries = entries;
          }
        }
      } catch (Exception e) {
        System.err.println("[BidHistoryController] Lỗi load: " + e.getMessage());
      }
      Platform.runLater(() -> {
        updateStats();
        applyTab(activeTab);
      });
    }, "bid-history-load").start();
  }

  @FXML
  private void handleTabAll() {
    applyTab("all");
  }

  @FXML
  private void handleTabWin() {
    applyTab("win");
  }

  @FXML
  private void handleTabLose() {
    applyTab("lose");
  }

  private void applyTab(String tab) {
    this.activeTab = tab;
    tabAll.setStyle(TAB_INACTIVE);
    tabWin.setStyle(TAB_INACTIVE);
    tabLose.setStyle(TAB_INACTIVE);
    List<BidHistoryEntry> source;
    switch (tab) {
      case "win" -> {
        tabWin.setStyle(TAB_ACTIVE);
        source = allEntries.stream()
            .filter(e -> "WIN".equalsIgnoreCase(e.getResult()))
            .collect(java.util.stream.Collectors.toList());
      }
      case "lose" -> {
        tabLose.setStyle(TAB_ACTIVE);
        source = allEntries.stream()
            .filter(e -> "LOSE".equalsIgnoreCase(e.getResult()))
            .collect(java.util.stream.Collectors.toList());
      }
      default -> {
        tabAll.setStyle(TAB_ACTIVE);
        source = new ArrayList<>(allEntries);
      }
    }
    historyList.setItems(FXCollections.observableArrayList(source));
  }

  private void updateStats() {
    long total = allEntries.size();
    long wins = allEntries.stream().filter(e -> "WIN".equalsIgnoreCase(e.getResult())).count();
    long loses = total - wins;
    String rate = total > 0 ? String.format("%.0f%%", wins * 100.0 / total) : "0%";
    if (rateLabel != null) {
      rateLabel.setText(rate);
    }
    if (totalLabel != null) {
      totalLabel.setText(String.valueOf(total));
    }
    if (winLabel != null) {
      winLabel.setText(String.valueOf(wins));
    }
    if (loseLabel != null) {
      loseLabel.setText(String.valueOf(loses));
    }
    if (subtitleLabel != null) {
      subtitleLabel.setText(total + " phiên đã tham gia");
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  private void leaveScreen() {
    removePushListener(); // BaseController
  }

  /**
   * .
   */
  @FXML
  public void handleProfile() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new ProfileView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleWatchlist() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new WatchlistView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleBalance() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new BalanceView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleNotification() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new NotificationView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleHome() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleBack() {
    leaveScreen();
    Stage stage = (Stage) historyList.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  /**
   * .
   */
  @FXML
  public void handleGoBalance() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new BalanceView(s, username).show();
    }
  }

  /**
   * .
   */
  @FXML
  public void handleSellerDashboard() {
    leaveScreen();
    Stage s = getStage();
    if (s != null) {
      new SellerView(s, username).show();
    }
  }

  /**
   * .
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
      return (Stage) historyList.getScene().getWindow();
    } catch (Exception e) {
      return null;
    }
  }

  // ── History cell (giữ nguyên) ─────────────────────────────────────────────
  private static class HistoryCell extends ListCell<BidHistoryEntry> {
    private final HBox card = new HBox(14);
    private final javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane();
    private final Label iconLabel = new Label();
    private final VBox content = new VBox(3);
    private final HBox titleRow = new HBox(8);
    private final Label itemName = new Label();
    private final Label badge = new Label();
    private final Label detail = new Label();
    private final HBox bottomRow = new HBox(12);
    private final Label timeLabel = new Label();
    private final Label priceLabel = new Label();

    HistoryCell() {
      iconWrap.setPrefSize(44, 44);
      iconWrap.setMinSize(44, 44);
      iconWrap.setMaxSize(44, 44);
      iconWrap.setAlignment(Pos.CENTER);
      iconLabel.setStyle("-fx-font-size: 20px;");
      iconWrap.getChildren().add(iconLabel);
      itemName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
      itemName.setMaxWidth(280);
      badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
          + " -fx-background-radius: 12; -fx-padding: 2 8;");
      detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
      timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
      priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
      titleRow.setAlignment(Pos.CENTER_LEFT);
      titleRow.getChildren().addAll(itemName, badge);
      bottomRow.setAlignment(Pos.CENTER_LEFT);
      bottomRow.getChildren().addAll(timeLabel, priceLabel);
      content.getChildren().addAll(titleRow, detail, bottomRow);
      HBox.setHgrow(content, Priority.ALWAYS);
      card.setAlignment(Pos.CENTER_LEFT);
      card.setPadding(new Insets(14, 14, 14, 16));
      card.getChildren().addAll(iconWrap, content);
      setStyle("-fx-background-color: transparent; -fx-padding: 0;");
      setText(null);
    }

    @Override
    protected void updateItem(BidHistoryEntry entry, boolean empty) {
      super.updateItem(entry, empty);
      setStyle("-fx-background-color: transparent; -fx-padding: 0;");
      if (empty || entry == null) {
        setGraphic(null);
        return;
      }

      iconLabel.setText("📦");
      iconWrap.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 22;");
      itemName.setText(entry.getItemName());
      detail.setText("ID: " + entry.getAuctionId());
      timeLabel.setText(entry.getEndTime() != null ? entry.getEndTime() : "");
      priceLabel.setText(AuctionUtils.formatPrice(entry.getFinalPrice())); // AuctionUtils

      boolean win = "WIN".equalsIgnoreCase(entry.getResult());
      if (win) {
        badge.setText("Thắng");
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
            + "-fx-background-radius: 12; -fx-padding: 2 8;"
            + "-fx-background-color: #D1FAE5; -fx-text-fill: #15803D;");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #15803D;");
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
            + "-fx-border-color: transparent transparent transparent #22C55E;"
            + "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 12 12 0;");
      } else {
        badge.setText("Thua");
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
            + "-fx-background-radius: 12; -fx-padding: 2 8;"
            + "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
            + "-fx-border-color: transparent transparent transparent #EF4444;"
            + "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 12 12 0;");
      }

      VBox outer = new VBox(card);
      outer.setPadding(new Insets(0, 0, 8, 0));
      setGraphic(outer);
      setText(null);
    }
  }
}