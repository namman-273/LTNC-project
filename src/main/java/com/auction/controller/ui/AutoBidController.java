package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.views.java.BidView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * autobidcontroller.
 */
public class AutoBidController extends BaseController implements Initializable {

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

  private String imageUrl = "";
  private String description = "";
  private String itemType = "";
  private double startingPrice = 0;
  private String sellerId = "";

  /**
   * setdata.
   */
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

    try {
      double price = Double.parseDouble(currentPrice.replaceAll("[^0-9.]", ""));
      long minStep = AuctionUtils.getMinimumIncrement(price); 
      if (minIncrementHintLabel != null) {
        minIncrementHintLabel.setText("💡 Bước tối thiểu: " + String.format("%,d VNĐ", minStep));
      }
    } catch (NumberFormatException ignored) {
      ignored.printStackTrace();
    }

    registerPushListener(this::handleServerPush); 
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
  }

  // ── Push Listener ────────────────────────────────────────────────────────
  private void handleServerPush(String message) {
    String[] parts = message.split("\\" + Protocol.SEPARATOR);
    if (parts.length == 0) {
      return;
    }

    switch (parts[0]) {

      case Protocol.NOTI_BALANCE_CHANGED:
        break;

      case Protocol.NOTI_BID_UPDATE:
        // Cập nhật currentPrice để khi handleBack() truyền về BidView giá luôn là mới
        // nhất
        if (parts.length >= 3 && parts[1].equals(auctionId)) {
          this.currentPrice = parts[2];
        }
        break;

      case Protocol.RES_END_SUCCESS:
        if (parts.length >= 2 && !parts[1].equals(auctionId)) {
          break;
        }
        String detail = parts.length >= 3 ? parts[2] : "";
        boolean isWin = detail.contains("Winner:" + username)
            || detail.contains("Winner: " + username);
        Platform.runLater(() -> {
          if (detail.contains("No winner")) {
            showMessage("Phiên kết thúc - không có người thắng.", "orange");
          } else if (isWin) {
            showMessage("🎉 Auto-bid đã giúp bạn thắng phiên!", "green");
            NotificationManager.getInstance().add(
                "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId, "auction", auctionId);
          } else {
            String winnerName = AuctionUtils.extractWinner(detail); // AuctionUtils
            String name = "N/A".equals(winnerName) ? "---" : winnerName;
            showMessage("Phiên kết thúc. Người chiến thắng: " + name, "orange");
            NotificationManager.getInstance().add(
                "⚠️ Phiên " + auctionId + " kết thúc. Bạn không thắng.", "auction", auctionId);
          }
        });
        removePushListener(); // BaseController
        break;

      case Protocol.NOTI_AUCTION_CANCELLED:
        if (parts.length >= 2 && parts[1].contains(auctionId)) {
          String cancelMsg = parts[1];
          Platform.runLater(() -> showMessage("❌ " + cancelMsg, "gray"));
          NotificationManager.getInstance().add(
              "❌ Phiên " + auctionId + " bị Admin hủy. Tiền đã được hoàn.", "auction", auctionId);
        }
        removePushListener(); // BaseController
        break;

      default:
        break;
    }
  }

  @FXML
  private void handleSetAutoBid() {
    String maxBid = maxBidField.getText().trim();
    String increment = incrementField.getText().trim();

    try {
      double maxBidVal = Double.parseDouble(maxBid);
      double incVal = Double.parseDouble(increment);
      double price = Double.parseDouble(currentPrice.replaceAll("[^0-9.]", ""));
      long minStep = AuctionUtils.getMinimumIncrement(price);
      if (incVal < minStep) {
        showMessage("❌ Bước tăng tối thiểu: " + String.format("%,d VNĐ", minStep), "red");
        return;
      }
      if (maxBidVal <= price) {
        showMessage("❌ Giá tối đa phải lớn hơn giá hiện tại: "
            + String.format("%,d VNĐ", (long) price), "red");
        return;
      }
    } catch (NumberFormatException e) {
      showMessage("❌ Vui lòng nhập số hợp lệ!", "red");
      return;
    }

    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
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
          showMessage("❌ " + (parts.length > 1 ? parts[1] : "Đặt auto-bid thất bại!"), "red");
        }
      });
    }).start();
  }

  @FXML
  private void handleBack() {
    // Remove listener TRƯỚC KHI show BidView để tránh duplicate listener
    removePushListener(); // BaseController
    Stage stage = (Stage) titleLabel.getScene().getWindow();
    // Truyền currentPrice đã được cập nhật từ NOTI_BID_UPDATE
    new BidView(stage, auctionId, itemName, currentPrice, status, username, endTime,
        imageUrl, description, itemType, startingPrice, sellerId).show();
  }

  private void showMessage(String msg, String color) {
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }
}