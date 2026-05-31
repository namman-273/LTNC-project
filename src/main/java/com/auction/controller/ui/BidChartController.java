package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.ToastManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Bidchartcontroller.
 */
public class BidChartController extends BaseController implements Initializable {

  @FXML
  private LineChart<Number, Number> bidChart;
  @FXML
  private Label titleLabel;

  private String auctionId;
  private String itemName;
  private String currentPrice;
  private String status;
  private String username;
  private long endTime;

  /**
 * setdata.
 */
  public void setData(String auctionId, String itemName, String currentPrice,
                      String status, String username, long endTime) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.currentPrice = currentPrice;
    this.status = status;
    this.username = username;
    this.endTime = endTime;

    initToastManager(titleLabel); 
    titleLabel.setText("Biểu đồ giá - " + itemName);
    // Lấy axes qua getter sau khi bidChart đã được inject
    if (bidChart.getXAxis() != null) {
      bidChart.getXAxis().setLabel("Lần đặt giá");
    }
    if (bidChart.getYAxis() != null) {
      bidChart.getYAxis().setLabel("Giá (VNĐ)");
    }
    loadChartData();
    registerPushListener(this::handlePushMessage); 
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    // bidChart chưa inject ở initialize() (inject sau), đặt label trong setData()
  }

  private void loadChartData() {
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId);
      System.out.println("Chart history: " + response);

      if (response == null || !response.startsWith(Protocol.RES_HISTORY)) {
        return;
      }

      String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
      if (parts.length < 3 || parts[2].trim().equals("[]")) {
        return;
      }

      try {
        JsonArray array = JsonParser.parseString(parts[2].trim()).getAsJsonArray();
        if (array.size() == 0) {
          return;
        }

        final int maxPoints = 20;
        int startIdx = Math.max(0, array.size() - maxPoints);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Giá đặt (20 lần gần nhất)");

        int displayIdx = 1;
        for (int i = startIdx; i < array.size(); i++) {
          JsonObject obj = array.get(i).getAsJsonObject();
          double amount = obj.has("amount") ? obj.get("amount").getAsDouble() : 0;
          final int idx = displayIdx++;
          series.getData().add(new XYChart.Data<>(idx, amount));
        }

        Platform.runLater(() -> {
          bidChart.getData().clear();
          if (!series.getData().isEmpty()) {
            bidChart.getData().add(series);
          }
        });
      } catch (Exception e) {
        System.err.println("Lỗi parse chart data: " + e.getMessage());
      }
    }).start();
  }

  private void handlePushMessage(String message) {
    if (message.startsWith(Protocol.NOTI_BID_UPDATE)) {
      String[] parts = message.split("\\" + Protocol.SEPARATOR);
      if (parts.length >= 3 && parts[1].equals(auctionId)) {
        try {
          double newPrice = Double.parseDouble(parts[2]);
          Platform.runLater(() -> {
            appendPoint(newPrice);
            ToastManager.show(ToastManager.Type.INFO,
                    "🔨 Giá mới: " + AuctionUtils.formatPrice(newPrice)); 
          });
        } catch (NumberFormatException ignored) {
          ignored.printStackTrace();
        }
      }
    }
  }

  private void appendPoint(double price) {
    if (bidChart.getData().isEmpty()) {
      return;
    }
    XYChart.Series<Number, Number> series = bidChart.getData().get(0);
    int nextIndex = series.getData().size() + 1;
    series.getData().add(new XYChart.Data<>(nextIndex, price));
  }

  @FXML
  private void handleBack() {
    removePushListener();
    Stage stage = (Stage) bidChart.getScene().getWindow();
    stage.close();
  }
}