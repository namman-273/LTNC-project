package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.ToastManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.AutoBidView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BidController extends BaseController implements Initializable {

  @FXML
  private StackPane rootPane;
  @FXML
  private Label auctionTitleLabel;
  @FXML
  private Label itemNameLabel;
  @FXML
  private Label itemTypeLabel;
  @FXML
  private Label sellerLabel;
  @FXML
  private Label currentPriceLabel;
  @FXML
  private Label statusLabel;
  @FXML
  private Label countdownLabel;
  @FXML
  private TextField bidAmountField;
  @FXML
  private ListView<HistoryEntry> bidHistoryList;
  @FXML
  private VBox snipingBox;
  @FXML
  private Label snipingCountdownLabel;
  @FXML
  private Label snipingCountLabel;
  @FXML
  private ImageView productImage;
  @FXML
  private VBox imagePlaceholder;
  @FXML
  private VBox descriptionBox;
  @FXML
  private Label messageLabel;
  @FXML
  private Label descriptionLabel;
  @FXML
  private Label minBidLabel;
  @FXML
  private Button toggleDetailBtn;
  @FXML
  private VBox productDetailPanel;
  @FXML
  private Label detailItemType;
  @FXML
  private Label detailStartPrice;
  @FXML
  private Label detailSeller;
  @FXML
  private Label balanceLabel;

  private String auctionId;
  private String username;
  private long endTime;
  private double currentPriceValue = 0;

  private String itemTypeCached = "";
  private double startingPriceCached = 0;
  private String imageUrlCached = "";
  private String descriptionCached = "";
  private String sellerIdCached = "";

  private final ConcurrentHashMap<String, ObservableList<HistoryEntry>> historyCache = new ConcurrentHashMap<>();

  private ObservableList<HistoryEntry> historyItems;
  private Timeline snipingTimeline;
  private Timeline countdownTimeline;
  private Timeline priceRefreshTimeline;

  private final AtomicBoolean historyLoading = new AtomicBoolean(false);
  private final AtomicBoolean pendingHistoryReload = new AtomicBoolean(false);

  // ── HistoryEntry DTO ─────────────────────────────────────────────────────
  public static class HistoryEntry {
    final String bidder;
    final double amount;
    final boolean isMe;
    final boolean isLeading;

    HistoryEntry(String bidder, double amount, boolean isMe, boolean isLeading) {
      this.bidder = bidder;
      this.amount = amount;
      this.isMe = isMe;
      this.isLeading = isLeading;
    }
  }

  // ── Custom cell ──────────────────────────────────────────────────────────
  private class HistoryCell extends ListCell<HistoryEntry> {
    private final HBox root = new HBox(12);
    private final Label avatar = new Label();
    private final VBox info = new VBox(3);
    private final HBox nameLine = new HBox(8);
    private final Label nameLabel = new Label();
    private final Label badge = new Label();
    private final Label priceLabel = new Label();

    HistoryCell() {
      avatar.setPrefSize(38, 38);
      avatar.setMinSize(38, 38);
      avatar.setAlignment(Pos.CENTER);
      avatar.setStyle("-fx-background-radius: 50%; -fx-font-size: 14px; -fx-font-weight: bold;");
      nameLine.setAlignment(Pos.CENTER_LEFT);
      nameLine.getChildren().addAll(nameLabel, badge);
      priceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
      HBox.setHgrow(info, Priority.ALWAYS);
      info.getChildren().addAll(nameLine);
      root.setAlignment(Pos.CENTER_LEFT);
      root.setPadding(new Insets(10, 14, 10, 14));
      root.getChildren().addAll(avatar, info, priceLabel);
      setGraphic(root);
      setText(null);
    }

    @Override
    protected void updateItem(HistoryEntry entry, boolean empty) {
      super.updateItem(entry, empty);
      if (empty || entry == null) {
        setGraphic(null);
        setStyle("");
        return;
      }

      String initials = entry.bidder.length() >= 2
          ? entry.bidder.substring(0, 2).toUpperCase()
          : entry.bidder.toUpperCase();
      avatar.setText(initials);

      if (entry.isMe) {
        avatar.setStyle(avatar.getStyle() + "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;");
        setStyle("-fx-background-color: #F0F7FF; -fx-background-radius: 10;");
      } else if (entry.isLeading) {
        avatar.setStyle(avatar.getStyle() + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
        setStyle("-fx-background-color: #F0FFF4; -fx-background-radius: 10;");
      } else {
        avatar.setStyle(avatar.getStyle() + "-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280;");
        setStyle("-fx-background-color: transparent;");
      }

      nameLabel.setText(entry.isMe ? "Bạn" : entry.bidder);
      nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

      if (entry.isLeading && entry.isMe) {
        badge.setText("Lượt của bạn");
        badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;"
            + "-fx-background-radius: 6; -fx-padding: 1 7;"
            + "-fx-font-size: 10px; -fx-font-weight: bold;");
        badge.setVisible(true);
      } else if (entry.isLeading) {
        badge.setText("Đang dẫn đầu");
        badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;"
            + "-fx-background-radius: 6; -fx-padding: 1 7;"
            + "-fx-font-size: 10px; -fx-font-weight: bold;");
        badge.setVisible(true);
      } else {
        badge.setText("");
        badge.setVisible(false);
      }

      priceLabel.setText(AuctionUtils.formatPrice(entry.amount));
      priceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;"
          + (entry.isLeading ? "-fx-text-fill: #1D4ED8;" : "-fx-text-fill: #374151;"));

      setGraphic(root);
      setText(null);
    }
  }

  // ── Setup ────────────────────────────────────────────────────────────────

  public void setData(String auctionId, String itemName, String currentPrice,
      String status, String username, long endTime) {
    setData(auctionId, itemName, currentPrice, status, username, endTime, "", "", "", 0, "");
  }

  public void setData(String auctionId, String itemName, String currentPrice,
      String status, String username, long endTime, String imageUrl, String description) {
    setData(auctionId, itemName, currentPrice, status, username, endTime,
        imageUrl, description, "", 0, "");
  }

  public void setData(String auctionId, String itemName, String currentPrice,
      String status, String username, long endTime,
      String imageUrl, String description,
      String itemType, double startingPrice, String sellerId) {
    this.auctionId = auctionId;
    this.username = username;
    this.endTime = endTime;
    this.itemTypeCached = itemType != null ? itemType : "";
    this.startingPriceCached = startingPrice;
    this.imageUrlCached = imageUrl != null ? imageUrl : "";
    this.descriptionCached = description != null ? description : "";
    this.sellerIdCached = sellerId != null ? sellerId : "";

    ToastManager.init(rootPane);

    auctionTitleLabel.setText(itemName);
    itemNameLabel.setText(itemName);
    currentPriceLabel.setText(AuctionUtils.formatPrice(currentPrice));
    statusLabel.setText(status);

    try {
      currentPriceValue = Double.parseDouble(
          currentPrice.replace(",", "").replace(" VND", "").replace(" VNĐ", "").trim());
    } catch (NumberFormatException ignored) {
      ignored.printStackTrace();
    }

    updateBidSuggestion(currentPriceValue);

    if (itemTypeLabel != null && !itemTypeCached.isEmpty()) {
      itemTypeLabel.setText(displayItemType(itemTypeCached));
    }

    if (sellerLabel != null && sellerId != null && !sellerId.isEmpty()) {
      sellerLabel.setText(sellerId);
    }

    System.out.println("[BidView] imageUrl=" + imageUrl);
    System.out.println("[BidView] description=" + description);

    String resolvedImageUrl = (imageUrl != null && !imageUrl.isEmpty()) ? imageUrl
        : (description != null && (description.startsWith("http")
            || description.startsWith("data:image"))) ? description : "";
    String resolvedDescription = (resolvedImageUrl.equals(description)) ? "" : description;

    if (!resolvedImageUrl.isEmpty() && productImage != null) {
      loadProductImage(resolvedImageUrl);
    }

    if (resolvedDescription != null && !resolvedDescription.isEmpty()
        && !resolvedDescription.startsWith("http")
        && !resolvedDescription.startsWith("data:image")) {
      if (descriptionLabel != null)
        descriptionLabel.setText(resolvedDescription);
      if (descriptionBox != null) {
        descriptionBox.setVisible(true);
        descriptionBox.setManaged(true);
      }
    }

    if (detailItemType != null) {
      detailItemType.setText(itemTypeCached.isEmpty() ? "—" : displayItemType(itemTypeCached));
    }
    if (detailStartPrice != null) {
      detailStartPrice.setText(startingPrice > 0 ? AuctionUtils.formatPrice(startingPrice) : "—");
    }
    if (detailSeller != null) {
      detailSeller.setText((sellerId != null && !sellerId.isEmpty()) ? sellerId : "—");
    }

    historyItems = historyCache.computeIfAbsent(auctionId, k -> FXCollections.observableArrayList());
    bidHistoryList.setItems(historyItems);
    bidHistoryList.setCellFactory(lv -> new HistoryCell());

    registerPushListener(this::handleServerPush); // BaseController
    startCountdown();
    startPriceRefresh();
    loadBalance(balanceLabel); // BaseController
    loadHistory();

    if (AuctionUtils.isFinishedStatus(status)) {
      Platform.runLater(() -> {
        statusLabel.setText("FINISHED");
        if (countdownTimeline != null)
          countdownTimeline.stop();
        if (countdownLabel != null) {
          countdownLabel.setText("⏰ Hết giờ!");
          countdownLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 28px;");
        }
        checkWinnerFromHistory();
      });
    }

    if (AuctionUtils.isCancelledStatus(status)) {
      Platform.runLater(this::handleCancelledState);
    }
  }

  /** Chuyển item type code sang text hiển thị. */
  private String displayItemType(String type) {
    return switch (type) {
      case "Art" -> "🎨 Nghệ thuật";
      case "Electronics" -> "⚡ Điện tử";
      case "Vehicle" -> "🚗 Xe cộ";
      default -> type;
    };
  }

  private void loadProductImage(String finalImageUrl) {
    new Thread(() -> {
      try {
        javafx.scene.image.Image img;
        if (finalImageUrl.startsWith("data:image")) {
          String base64 = finalImageUrl.substring(finalImageUrl.indexOf(",") + 1);
          byte[] bytes = java.util.Base64.getDecoder().decode(base64);
          img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
        } else {
          java.net.URL url = java.net.URI.create(finalImageUrl).toURL();
          java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
          conn.setRequestProperty("User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
          conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*");
          conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
          conn.setRequestProperty("Referer", url.getProtocol() + "://" + url.getHost());
          conn.setInstanceFollowRedirects(true);
          conn.setConnectTimeout(8000);
          conn.setReadTimeout(8000);
          conn.connect();
          try (java.io.InputStream is = conn.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
          } finally {
            conn.disconnect();
          }
        }
        if (!img.isError()) {
          final javafx.scene.image.Image finalImg = img;
          Platform.runLater(() -> {
            productImage.setImage(finalImg);
            productImage.setVisible(true);
            productImage.setManaged(true);
            if (imagePlaceholder != null) {
              imagePlaceholder.setVisible(false);
              imagePlaceholder.setManaged(false);
            }
          });
        }
      } catch (Exception e) {
        System.err.println("Không load được ảnh: " + e.getMessage());
      }
    }).start();
  }

  private void checkWinnerFromHistory() {
    new Thread(() -> {
      String response = ServerConnection.getInstance()
          .sendAndReceive(Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId);
      if (response == null || response.startsWith("ERROR")
          || !response.startsWith(Protocol.RES_HISTORY)) {
        Platform.runLater(this::checkWinnerFromHistoryCache);
        return;
      }
      String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
      if (parts.length < 3 || parts[2].trim().isEmpty() || parts[2].trim().equals("[]")) {
        Platform.runLater(() -> showInfo("Phiên kết thúc - không có người đặt giá."));
        return;
      }
      try {
        JsonArray array = JsonParser.parseString(parts[2].trim()).getAsJsonArray();
        if (array.size() == 0) {
          Platform.runLater(() -> showInfo("Phiên kết thúc - không có người đặt giá."));
          return;
        }
        JsonObject lastBid = array.get(array.size() - 1).getAsJsonObject();
        String winnerName = extractBidderName(lastBid);
        Platform.runLater(() -> {
          historyItems.clear();
          for (int i = array.size() - 1; i >= 0; i--) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String bidder = extractBidderName(obj);
            double amount = obj.has("amount") ? obj.get("amount").getAsDouble() : 0;
            boolean isMe = bidder.equals(username);
            boolean isLeading = (i == array.size() - 1);
            historyItems.add(new HistoryEntry(bidder, amount, isMe, isLeading));
          }
          if (winnerName.equals(username)) {
            showSuccess("🎉 Chúc mừng! Bạn đã thắng phiên đấu giá!");
            NotificationManager.getInstance().add(
                "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId, "auction", auctionId);
          } else {
            showInfo("Phiên kết thúc. Người chiến thắng: " + winnerName);
          }
        });
      } catch (Exception e) {
        System.err.println("Lỗi parse history khi check winner: " + e.getMessage());
        Platform.runLater(this::checkWinnerFromHistoryCache);
      }
    }, "check-winner-thread").start();
  }

  /** Rút tên bidder từ JsonObject bid. */
  private String extractBidderName(JsonObject obj) {
    if (obj.has("bidder") && obj.get("bidder").isJsonObject()) {
      JsonObject bo = obj.get("bidder").getAsJsonObject();
      if (bo.has("username"))
        return bo.get("username").getAsString();
    }
    return "---";
  }

  private void checkWinnerFromHistoryCache() {
    if (historyItems == null || historyItems.isEmpty()) {
      showInfo("Phiên đã kết thúc.");
      return;
    }
    HistoryEntry leading = historyItems.get(0);
    if (leading.isMe) {
      showSuccess("🎉 Chúc mừng! Bạn đã thắng phiên đấu giá!");
      NotificationManager.getInstance().add(
          "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId, "auction", auctionId);
    } else {
      showInfo("Phiên kết thúc. Người chiến thắng: " + leading.bidder);
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
  }

  @FXML
  private void handleToggleDetail() {
    if (productDetailPanel == null)
      return;
    boolean show = !productDetailPanel.isVisible();
    productDetailPanel.setVisible(show);
    productDetailPanel.setManaged(show);
    if (toggleDetailBtn != null) {
      toggleDetailBtn.setText(show ? "▲ Ẩn chi tiết sản phẩm" : "▼ Xem chi tiết sản phẩm");
    }
  }

  // ── Push Listener ────────────────────────────────────────────────────────
  private void handleServerPush(String message) {
    System.out.println("PUSH RECEIVED: " + message);
    String[] parts = message.split("\\" + Protocol.SEPARATOR);
    if (parts.length == 0)
      return;

    switch (parts[0]) {

      case Protocol.NOTI_BALANCE_CHANGED:
        if (parts.length >= 2) {
          String newBal = parts[1];
          Platform.runLater(() -> updateBalanceLabelFromPush(balanceLabel, newBal)); // BaseController
        }
        break;

      case Protocol.NOTI_BID_UPDATE:
        if (parts.length >= 4 && parts[1].equals(auctionId)) {
          String newPrice = parts[2];
          String bidder = parts[3];
          Platform.runLater(() -> {
            try {
              currentPriceValue = Double.parseDouble(newPrice);
            } catch (Exception ignored) {
              ignored.printStackTrace();
            }
            currentPriceLabel.setText(AuctionUtils.formatPrice(newPrice));
            updateBidSuggestion(currentPriceValue);
            if (!bidder.equals(username)) {
              showWarning(bidder + " vừa đặt " + AuctionUtils.formatPrice(newPrice));
            }
            double amt = currentPriceValue;
            boolean isMe = bidder.equals(username);
            if (!historyItems.isEmpty()) {
              HistoryEntry old = historyItems.get(0);
              historyItems.set(0, new HistoryEntry(old.bidder, old.amount, old.isMe, false));
            }
            historyItems.add(0, new HistoryEntry(bidder, amt, isMe, true));
          });
        }
        break;

      case Protocol.NOTI_SNIPING_UPDATE:
        if (parts.length >= 4 && parts[1].equals(auctionId)) {
          String count = parts[3];
          try {
            long newEndTime = Long.parseLong(parts[2]);
            this.endTime = newEndTime;
            Platform.runLater(() -> showSnipingAlert(count));
            NotificationManager.getInstance().add(
                "⏱ Phiên " + auctionId + " được gia hạn lần " + count, "auction", auctionId);
          } catch (NumberFormatException ignored) {
            ignored.printStackTrace();
          }
        }
        break;

      case Protocol.NOTI_OUTBID:
        if (parts.length >= 4 && parts[1].equals(auctionId)) {
          String newBidder = parts[2];
          String newAmt = parts[3];
          Platform.runLater(() -> {
            try {
              double amt = Double.parseDouble(newAmt);
              currentPriceValue = amt;
              currentPriceLabel.setText(AuctionUtils.formatPrice(newAmt));
              updateBidSuggestion(amt);
              boolean alreadyAppended = !historyItems.isEmpty()
                  && historyItems.get(0).bidder.equals(newBidder)
                  && historyItems.get(0).amount == amt;
              if (!alreadyAppended) {
                if (!historyItems.isEmpty()) {
                  HistoryEntry prev = historyItems.get(0);
                  historyItems.set(0, new HistoryEntry(prev.bidder, prev.amount, prev.isMe, false));
                }
                historyItems.add(0, new HistoryEntry(newBidder, amt, false, true));
              }
            } catch (NumberFormatException ignored) {
              ignored.printStackTrace();
            }
            showWarning("⚠️ Bị vượt giá bởi " + newBidder + "!");
          });
          NotificationManager.getInstance().add(
              "⚠️ Bị vượt giá trong phiên " + auctionId
                  + " — Giá mới: " + AuctionUtils.formatPrice(newAmt),
              "auction", auctionId);
        }
        break;

      case Protocol.NOTI_REFUND:
        if (parts.length >= 4 && parts[1].equals(auctionId)) {
          String refundAmt = parts[2];
          String newBal = parts[3];
          Platform.runLater(() -> showInfo("💰 Hoàn tiền vào ví"));
          NotificationManager.getInstance().add(
              "Hoàn " + AuctionUtils.formatPrice(refundAmt)
                  + " → Số dư: " + AuctionUtils.formatPrice(newBal),
              "balance", auctionId);
        }
        break;

      case Protocol.RES_END_SUCCESS:
        if (parts.length >= 2 && !parts[1].equals(auctionId))
          break;
        Platform.runLater(() -> {
          statusLabel.setText("FINISHED");
          stopSnipingCountdown();
          if (countdownTimeline != null)
            countdownTimeline.stop();
          if (priceRefreshTimeline != null)
            priceRefreshTimeline.stop();
          if (countdownLabel != null) {
            countdownLabel.setText("⏰ Hết giờ!");
            countdownLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 28px;");
          }
          String detail = parts.length >= 3 ? parts[2] : "";
          if (detail.contains("No winner")) {
            showInfo("Phiên kết thúc - không có người thắng.");
            NotificationManager.getInstance().add(
                "⚠️ Phiên " + auctionId + " kết thúc. Không có người thắng.", "auction", auctionId);
          } else {
            boolean isWin = detail.contains("Winner:" + username)
                || detail.contains("Winner: " + username);
            if (isWin) {
              showSuccess("🎉 Chúc mừng! Bạn đã thắng phiên đấu giá!");
              NotificationManager.getInstance().add(
                  "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId, "auction", auctionId);
            } else {
              String winnerDisplay = AuctionUtils.extractWinner(detail); // AuctionUtils
              String name = "N/A".equals(winnerDisplay) ? "---" : winnerDisplay;
              showInfo("Phiên kết thúc. Người chiến thắng: " + name);
              NotificationManager.getInstance().add(
                  "🔔 Phiên " + auctionId + " đã kết thúc. Người thắng: " + name,
                  "auction", auctionId);
            }
          }
        });
        removePushListener(); // BaseController
        break;

      case Protocol.NOTI_AUCTION_CANCELLED:
        if (parts.length >= 2) {
          String reason = parts.length >= 3 ? parts[2] : "Admin hủy";
          String notiMsg = "❌ Phiên " + auctionId + " " + reason + ". Tiền đã được hoàn.";
          Platform.runLater(() -> {
            handleCancelledState();
            showWarning("❌ " + reason);
          });
          NotificationManager.getInstance().add(notiMsg, "auction", auctionId);
        }
        removePushListener(); // BaseController
        break;

      default:
        break;
    }
  }

  private void handleCancelledState() {
    stopSnipingCountdown();
    if (countdownTimeline != null)
      countdownTimeline.stop();
    if (priceRefreshTimeline != null)
      priceRefreshTimeline.stop();
    if (statusLabel != null)
      statusLabel.setText("CANCELED");
    if (countdownLabel != null) {
      countdownLabel.setText("❌ Bị hủy!");
      countdownLabel.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold; -fx-font-size: 28px;");
    }
  }

  // ── Quick-add ────────────────────────────────────────────────────────────
  @FXML
  private void handleAdd1M() {
    addAmount(1_000_000);
  }

  @FXML
  private void handleAdd5M() {
    addAmount(5_000_000);
  }

  @FXML
  private void handleAdd10M() {
    addAmount(10_000_000);
  }

  private void addAmount(double amount) {
    try {
      String current = bidAmountField.getText().trim();
      double base = current.isEmpty() ? currentPriceValue
          : Double.parseDouble(current.replace(",", "").replace(" VND", "").trim());
      bidAmountField.setText(String.valueOf((long) (base + amount)));
    } catch (NumberFormatException ignored) {
      bidAmountField.setText(String.valueOf((long) (currentPriceValue + amount)));
    }
  }

  private void updateBidSuggestion(double price) {
    long minIncrement = AuctionUtils.getMinimumIncrement(price); // AuctionUtils
    long minBid = (long) price + minIncrement;
    if (bidAmountField != null) {
      bidAmountField.setPromptText(String.format("%,d", minBid));
    }
    if (minBidLabel != null) {
      minBidLabel.setText("💡 Giá tối thiểu: " + String.format("%,d VNĐ", minBid));
    }
  }

  // ── Countdown ────────────────────────────────────────────────────────────
  private void startCountdown() {
    if (countdownTimeline != null)
      countdownTimeline.stop();
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
      long remaining = endTime - System.currentTimeMillis();
      if (countdownLabel == null)
        return;
      if (remaining <= 0) {
        countdownLabel.setText("⏰ Hết giờ!");
        countdownLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 28px;");
        countdownTimeline.stop();
        return;
      }
      long hours = remaining / 3_600_000;
      long minutes = (remaining % 3_600_000) / 60_000;
      long seconds = (remaining % 60_000) / 1_000;
      countdownLabel.setText(String.format("%02d : %02d : %02d", hours, minutes, seconds));
      countdownLabel.setStyle(remaining < 300_000
          ? "-fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 28px; -fx-font-family: monospace;"
          : "-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-font-size: 28px; -fx-font-family: monospace;");
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  // ── Sniping ──────────────────────────────────────────────────────────────
  private void showSnipingAlert(String extensionCount) {
    if (snipingBox == null)
      return;
    snipingBox.setVisible(true);
    snipingBox.setManaged(true);
    snipingCountLabel.setText("Lần gia hạn thứ: " + extensionCount);
    snipingCountdownLabel.setText("⏱ +2 phút vừa được cộng thêm!");
    showWarning("Phiên gia hạn lần " + extensionCount + " (+2 phút)");
    if (snipingTimeline != null)
      snipingTimeline.stop();
    snipingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> stopSnipingAlert()));
    snipingTimeline.setCycleCount(1);
    snipingTimeline.play();
  }

  private void stopSnipingAlert() {
    if (snipingTimeline != null) {
      snipingTimeline.stop();
      snipingTimeline = null;
    }
    if (snipingBox != null) {
      snipingBox.setVisible(false);
      snipingBox.setManaged(false);
    }
  }

  private void stopSnipingCountdown() {
    stopSnipingAlert();
  }

  // ── History ──────────────────────────────────────────────────────────────
  private void loadHistory() {
    if (!historyLoading.compareAndSet(false, true)) {
      pendingHistoryReload.set(true);
      return;
    }
    pendingHistoryReload.set(false);

    new Thread(() -> {
      try {
        String response = ServerConnection.getInstance().sendAndReceive(
            Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId);
        if (response == null || response.startsWith("ERROR")
            || !response.startsWith(Protocol.RES_HISTORY))
          return;

        String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
        if (parts.length < 3)
          return;

        String json = parts[2].trim();
        if (json.isEmpty() || json.equals("[]"))
          return;

        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        Platform.runLater(() -> {
          historyItems.clear();
          for (int i = array.size() - 1; i >= 0; i--) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String bidder = extractBidderName(obj);
            double amount = obj.has("amount") ? obj.get("amount").getAsDouble() : 0;
            boolean isMe = bidder.equals(username);
            boolean isLeading = (i == array.size() - 1);
            historyItems.add(new HistoryEntry(bidder, amount, isMe, isLeading));
          }
        });
      } catch (Exception e) {
        System.err.println("Lỗi parse history: " + e.getMessage());
      } finally {
        historyLoading.set(false);
        if (pendingHistoryReload.compareAndSet(true, false)) {
          loadHistory();
        }
      }
    }).start();
  }

  // ── Actions ──────────────────────────────────────────────────────────────
  @FXML
  private void handleBid() {
    String amountStr = bidAmountField.getText().trim();
    if (amountStr.isEmpty()) {
      showWarning("Vui lòng nhập giá!");
      return;
    }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_BID + Protocol.SEPARATOR + auctionId + Protocol.SEPARATOR + amountStr);
      Platform.runLater(() -> {
        if (response == null || response.startsWith("ERROR|Mất kết nối")) {
          showError("Mất kết nối server!");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_BID_SUCCESS)) {
          showSuccess("Đặt giá thành công!");
          bidAmountField.clear();
          if (parts.length > 2) {
            try {
              double newPrice = Double.parseDouble(parts[2]);
              currentPriceValue = newPrice;
              currentPriceLabel.setText(AuctionUtils.formatPrice(newPrice));
              updateBidSuggestion(newPrice);
            } catch (NumberFormatException ignored) {
              ignored.printStackTrace();
            }
          }
          if (!historyItems.isEmpty()) {
            HistoryEntry prev = historyItems.get(0);
            historyItems.set(0, new HistoryEntry(prev.bidder, prev.amount, prev.isMe, false));
          }
          historyItems.add(0, new HistoryEntry(username, currentPriceValue, true, true));
        } else {
          showError(parts.length > 1 ? parts[1] : "Đặt giá thất bại!");
        }
      });
    }).start();
  }

  @FXML
  private void handleAutoBid() {
    stopAll();
    Stage stage = (Stage) bidAmountField.getScene().getWindow();
    new AutoBidView(stage, auctionId, itemNameLabel.getText(),
        currentPriceLabel.getText(), statusLabel.getText(),
        username, endTime,
        imageUrlCached, descriptionCached, itemTypeCached, startingPriceCached, sellerIdCached)
        .show();
  }

  @FXML
  private void handleBack() {
    stopAll();
    Platform.runLater(() -> {
      Stage stage = (Stage) bidAmountField.getScene().getWindow();
      new AuctionListView(stage, username).show();
    });
  }

  @FXML
  private void handleViewChart() {
    try {
      javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
          getClass().getResource("/com/auction/views/fxml/BidChartView.fxml"));
      javafx.scene.Parent root = loader.load();
      BidChartController chartCtrl = loader.getController();
      chartCtrl.setData(auctionId, itemNameLabel.getText(),
          currentPriceLabel.getText(), statusLabel.getText(), username, endTime);
      javafx.stage.Stage chartStage = new javafx.stage.Stage();
      chartStage.setTitle("Biểu đồ giá - " + itemNameLabel.getText());
      chartStage.setScene(new javafx.scene.Scene(root, 700, 450));
      chartStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
      chartStage.initOwner((Stage) bidAmountField.getScene().getWindow());
      chartStage.setResizable(true);
      chartStage.show();
    } catch (Exception e) {
      System.err.println("Lỗi mở chart: " + e.getMessage());
    }
  }

  private void startPriceRefresh() {
    if (priceRefreshTimeline != null)
      priceRefreshTimeline.stop();
    priceRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
      new Thread(() -> {
        String resp = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
        if (resp == null || !resp.startsWith(Protocol.RES_LIST_SUCCESS))
          return;
        String json = resp.substring(resp.indexOf(Protocol.SEPARATOR) + 1);
        try {
          com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
          boolean found = false;
          for (com.google.gson.JsonElement el : arr) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            if (obj.has("id") && auctionId.equals(obj.get("id").getAsString())) {
              found = true;
              double price = obj.has("currentPrice") ? obj.get("currentPrice").getAsDouble() : 0;
              String status = obj.has("status") ? obj.get("status").getAsString() : "";
              Platform.runLater(() -> {
                if (price > currentPriceValue) {
                  currentPriceValue = price;
                  currentPriceLabel.setText(AuctionUtils.formatPrice(price));
                  updateBidSuggestion(price);
                  loadHistory();
                }
                if (statusLabel != null && !status.isEmpty())
                  statusLabel.setText(status);
                if (AuctionUtils.isCancelledStatus(status)) {
                  if (priceRefreshTimeline != null)
                    priceRefreshTimeline.stop();
                  handleCancelledState();
                }
              });
              break;
            }
          }
          if (!found) {
            Platform.runLater(() -> {
              if (priceRefreshTimeline != null)
                priceRefreshTimeline.stop();
            });
          }
        } catch (Exception ex) {
          System.err.println("[BidView] Lỗi poll giá: " + ex.getMessage());
        }
      }, "price-refresh-thread").start();
    }));
    priceRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    priceRefreshTimeline.play();
  }

  private void stopAll() {
    removePushListener(); // BaseController
    stopSnipingCountdown();
    if (countdownTimeline != null)
      countdownTimeline.stop();
    if (priceRefreshTimeline != null)
      priceRefreshTimeline.stop();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────
  private void showError(String msg) {
    ToastManager.show(ToastManager.Type.DANGER, msg);
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }

  private void showSuccess(String msg) {
    ToastManager.show(ToastManager.Type.SUCCESS, msg);
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }

  private void showInfo(String msg) {
    ToastManager.show(ToastManager.Type.INFO, msg);
  }

  private void showWarning(String msg) {
    ToastManager.show(ToastManager.Type.WARNING, msg);
  }
}