package com.auction.controller.ui;

import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.views.java.BidView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AutoBidController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label currentAutoBidLabel;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label messageLabel;

    private String auctionId;
    private String itemName;
    private String currentPrice;
    private String status;
    private String username;
    private long endTime;

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username, long endTime) {
        this.endTime      = endTime;
        this.auctionId    = auctionId;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.username     = username;
        titleLabel.setText("🤖 Auto-Bid - " + itemName);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    @FXML
    private void handleSetAutoBid() {
        // Không tự validate — gửi thẳng lên BE
        String maxBid    = maxBidField.getText().trim();
        String increment = incrementField.getText().trim();

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_ADD_AUTO_BID + Protocol.SEPARATOR
                            + auctionId  + Protocol.SEPARATOR
                            + maxBid     + Protocol.SEPARATOR
                            + increment
            );
            System.out.println("AutoBid response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_AUTO_BID_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Đặt auto-bid thành công!";
                    showMessage("✅ " + msg, "green");

                    // Hiện thông tin auto-bid vừa đặt — dùng try-catch đề phòng giá trị lạ
                    try {
                        double maxBidVal   = Double.parseDouble(maxBid);
                        double incrementVal = Double.parseDouble(increment);
                        currentAutoBidLabel.setText(
                                "Giá tối đa: " + String.format("%,.0f VNĐ", maxBidVal)
                                        + " | Bước tăng: " + String.format("%,.0f VNĐ", incrementVal)
                        );
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
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        new BidView(stage, auctionId, itemName, currentPrice, status, username, endTime).show();
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }
}