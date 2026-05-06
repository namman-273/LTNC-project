package com.auction.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.BidView;

import java.net.URL;
import java.util.ResourceBundle;

public class BidChartController implements Initializable {

    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label titleLabel;

    private String auctionId, itemName, currentPrice, status, username;

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.username = username;
        titleLabel.setText("Biểu đồ giá - " + itemName);
        loadChartData();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        xAxis.setLabel("Lần đặt giá");
        yAxis.setLabel("Giá (VND)");
    }

    private void loadChartData() {
        new Thread(() -> {
            ServerConnection conn = new ServerConnection("localhost", 9999);
            try {
                String pwd = SessionManager.getInstance().getPassword();
                if (!conn.connectDirect()) return;
                conn.sendAndReceive("LOGIN|" + username + "|" + pwd);
                String response = conn.sendAndReceive("GET_HISTORY|" + auctionId);
                System.out.println("Chart history: " + response);

                if (response == null || !response.contains("HISTORY_RES")) return;

                String[] parts = response.split("\\|", 3);
                if (parts.length < 3) return;

                String json = parts[2].trim();
                if (json.equals("[]") || json.isEmpty()) return;

                // Parse an toàn hơn regex — tìm "amount": trực tiếp
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Giá đặt");

                // Tách từng object JSON bằng "amount":
                String[] tokens = json.split("\"amount\":");
                int index = 1;
                for (int i = 1; i < tokens.length; i++) {
                    try {
                        // Lấy số ngay sau "amount":
                        String numStr = tokens[i].split("[,}]")[0].trim();
                        double price = Double.parseDouble(numStr);
                        final int idx = index++;
                        series.getData().add(new XYChart.Data<>(idx, price));
                    } catch (Exception e) {
                        System.err.println("Chart parse error at entry " + i + ": " + e.getMessage());
                    }
                }

                final XYChart.Series<Number, Number> finalSeries = series;
                Platform.runLater(() -> {
                    bidChart.getData().clear();
                    if (!finalSeries.getData().isEmpty()) {
                        bidChart.getData().add(finalSeries);
                    }
                });

            } catch (Exception e) {
                System.err.println("Lỗi load chart: " + e.getMessage());
            } finally {
                conn.disconnectDirect();
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) bidChart.getScene().getWindow();
        new BidView(stage, auctionId, itemName, currentPrice, status, username).show();
    }
}