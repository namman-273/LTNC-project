package com.auction.controller.ui;

import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.util.ui.NotificationManager;
import com.auction.views.java.BidView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AutoBidController implements Initializable {

  @FXML
  private Label titleLabel;
  @FXML
  private Label currentAutoBidLabel;
  @FXML
  private TextField maxBidField;
  @FXML
  private TextField incrementField;
  @FXML
  private Label messageLabel;
  @FXML
  private Label minIncrementHintLabel;

  private String auctionId;
  private String itemName;
  private String currentPrice;
  private String status;
  private String username;
  private long endTime;

  // FIX: lưu đủ thông tin để truyền lại BidView khi back
  private String imageUrl = "";
  private String description = "";
  private String itemType = "";
  private double startingPrice = 0;
  private String sellerId = "";

  private Consumer<String> pushListener;

  /** Được gọi từ AutoBidView — truyền đủ thông tin sản phẩm */
  public void setData(String auctionId, String itemName, String currentPrice,
      String status, String username, long endTime,
      String imageUrl, String description,
      String itemType, double startingPrice, String sellerId) {
    this.endTime = endTime;
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.currentPrice = currentPrice;
    this.status = status;
    this.username = username;
    this.imageUrl = imageUrl != null ? imageUrl : "";
    this.description = description != null ? description : "";
    this.itemType = itemType != null ? itemType : "";
    this.startingPrice = startingPrice;
    this.sellerId = sellerId != null ? sellerId : "";
    titleLabel.setText("🤖 Auto-Bid - " + itemName);

    // Hiện gợi ý bước tăng tối thiểu dựa trên giá hiện tại
    try {
      double price = Double.parseDouble(currentPrice.replaceAll("[^0-9.]", ""));
      long minStep = getMinimumIncrement(price);
      if (minIncrementHintLabel != null) {
        minIncrementHintLabel.setText("💡 Bước tối thiểu: " + String.format("%,d VNĐ", minStep));
      }
    } catch (NumberFormatException ignored) {
    }

    // FIX: lắng nghe push ngay cả khi đang ở màn AutoBid
    registerPushListener();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
  }

  // ── Push Listener ────────────────────────────────────────────────────────
  private void registerPushListener() {
    pushListener = this::handleServerPush;
    ServerConnection.getInstance().addPushListener(pushListener);
  }

  private void removePushListener() {
    if (pushListener != null) {
      ServerConnection.getInstance().removePushListener(pushListener);
      pushListener = null;
    }
  }

  private void handleServerPush(String message) {
    String[] parts = message.split("\\" + Protocol.SEPARATOR);
    if (parts.length == 0)
      return;

    switch (parts[0]) {

      case Protocol.NOTI_BALANCE_CHANGED:
        // Không có balanceLabel ở màn này nhưng không bỏ lỡ event
        break;

      // FIX Lỗi 2: cập nhật currentPrice khi nhận NOTI_BID_UPDATE,
      // để khi handleBack() truyền về BidView giá luôn là mới nhất
      case Protocol.NOTI_BID_UPDATE:
        if (parts.length >= 3 && parts[1].equals(auctionId)) {
          this.currentPrice = parts[2]; // cập nhật cache currentPrice
        }
        break;

      // FIX: nhận kết quả phiên ngay khi đang ở màn AutoBid
      case Protocol.RES_END_SUCCESS:
        if (parts.length >= 2 && !parts[1].equals(auctionId))
          break;
        String detail = parts.length >= 3 ? parts[2] : "";
        boolean isWin = detail.contains("Winner:" + username)
            || detail.contains("Winner: " + username);
        Platform.runLater(() -> {
          if (detail.contains("No winner")) {
            showMessage("Phiên kết thúc - không có người thắng.", "orange");
          } else if (isWin) {
            showMessage("🎉 Auto-bid đã giúp bạn thắng phiên!", "green");
            NotificationManager.getInstance().add(
                "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId,
                "auction", auctionId);
          } else {
            String winnerName = "";
            if (detail.contains("Winner: ")) {
              winnerName = detail.substring(detail.indexOf("Winner: ") + 8).trim();
            } else if (detail.contains("Winner:")) {
              winnerName = detail.substring(detail.indexOf("Winner:") + 7).trim();
            }
            int sep = winnerName.indexOf("|");
            if (sep >= 0)
              winnerName = winnerName.substring(0, sep).trim();
            showMessage("Phiên kết thúc. Người chiến thắng: "
                + (winnerName.isEmpty() ? "---" : winnerName), "orange");
            NotificationManager.getInstance().add(
                "⚠️ Phiên " + auctionId + " kết thúc. Bạn không thắng.",
                "auction", auctionId);
          }
        });
        removePushListener();
        break;

      case Protocol.NOTI_AUCTION_CANCELLED:
        if (parts.length >= 2 && parts[1].contains(auctionId)) {
          String cancelMsg = parts[1];
          Platform.runLater(() -> showMessage("\u274c " + cancelMsg, "gray"));
          NotificationManager.getInstance().add(
              "\u274c Phi\u00ean " + auctionId
                  + " b\u1ecb Admin h\u1ee7y. Ti\u1ec1n \u0111\u00e3 \u0111\u01b0\u1ee3c ho\u00e0n.",
              "auction", auctionId);
        }
        removePushListener();
        break;

      default:
        break;
    }
  }

  @FXML
  private void handleSetAutoBid() {
    String maxBid = maxBidField.getText().trim();
    String increment = incrementField.getText().trim();

    // Validate FE trước khi gửi
    try {
      double maxBidVal = Double.parseDouble(maxBid);
      double incVal = Double.parseDouble(increment);
      double price = Double.parseDouble(currentPrice.replaceAll("[^0-9.]", ""));
      long minStep = getMinimumIncrement(price);
      if (incVal < minStep) {
        showMessage("❌ Bước tăng tối thiểu: " + String.format("%,d VNĐ", minStep), "red");
        return;
      }
      if (maxBidVal <= price) {
        showMessage("❌ Giá tối đa phải lớn hơn giá hiện tại: " + String.format("%,d VNĐ", (long) price), "red");
        return;
      }
    } catch (NumberFormatException e) {
      showMessage("❌ Vui lòng nhập số hợp lệ!", "red");
      return;
    }

    new Thread(() -> {
      ServerConnection conn = ServerConnection.getInstance();
      String response = conn.sendAndReceive(
          Protocol.CMD_ADD_AUTO_BID + Protocol.SEPARATOR
              + auctionId + Protocol.SEPARATOR
              + maxBid + Protocol.SEPARATOR
              + increment);
      System.out.println("AutoBid response: " + response);

      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_AUTO_BID_SUCCESS)) {
          String msg = parts.length > 1 ? parts[1] : "Đặt auto-bid thành công!";
          showMessage("✅ " + msg, "green");
          try {
            double maxBidVal = Double.parseDouble(maxBid);
            double incrementVal = Double.parseDouble(increment);
            currentAutoBidLabel.setText(
                "Giá tối đa: " + String.format("%,.0f VNĐ", maxBidVal)
                    + " | Bước tăng: " + String.format("%,.0f VNĐ", incrementVal));
          } catch (NumberFormatException e) {
            currentAutoBidLabel.setText("Đã đặt auto-bid thành công.");
          }
          maxBidField.clear();
          incrementField.clear();
        } else {
          String msg = parts.length > 1 ? parts[1] : "Đặt auto-bid thất bại!";
          showMessage("❌ " + msg, "red");
        }
      });
    }).start();
  }

  @FXML
  private void handleBack() {
    // FIX Lỗi 3: remove listener TRƯỚC KHI show BidView để
    // BidView.registerPushListener()
    // không bị duplicate với listener của AutoBidController còn sót lại
    removePushListener();
    // FIX Lỗi 2: truyền currentPrice đã được cập nhật từ NOTI_BID_UPDATE
    // để BidView hiển thị đúng giá mới nhất ngay khi quay về
    Stage stage = (Stage) titleLabel.getScene().getWindow();
    new BidView(stage, auctionId, itemName, currentPrice, status, username, endTime,
        imageUrl, description, itemType, startingPrice, sellerId).show();
  }

  /** Mirror AuctionValidator.getMinimumIncrement */
  private long getMinimumIncrement(double price) {
    if (price < 1_000_000)
      return 50_000;
    if (price < 5_000_000)
      return 100_000;
    if (price < 10_000_000)
      return 250_000;
    return 500_000;
  }

  private void showMessage(String msg, String color) {
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }
}
