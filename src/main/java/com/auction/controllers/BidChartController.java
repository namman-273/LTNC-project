package com.auction.controllers;

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
        this.auctionId = auctionId; this.itemName = itemName;
        this.currentPrice = currentPrice; this.status = status;
        this.username = username;
        titleLabel.setText("Biểu đồ giá - " + itemName);
        loadChartData();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    private void loadChartData() {
        new Thread(() -> {
            ServerConnection conn = new ServerConnection("localhost", 9999);
            try {
                String pwd = SessionManager.getInstance().getPassword();
                if (!conn.connectDirect()) return;
                conn.sendAndReceive("LOGIN|" + username + "|" + pwd);
                String response = conn.sendAndReceive("GET_HISTORY|" + auctionId);
                System.out.println("Chart history: " + response);

                if (response != null && response.contains("HISTORY_RES")) {
                    String[] parts = response.split("\\|", 3);
                    if (parts.length >= 3) {
                        String json = parts[2].trim();
                        if (!json.equals("[]") && !json.isEmpty()) {
                            String[] entries = json.substring(1, json.length() - 1).split("\\},\\{");
                            XYChart.Series<Number, Number> series = new XYChart.Series<>();
                            series.setName("Giá đặt");

                            for (int i = 0; i < entries.length; i++) {
                                try {
                                    String amount = entries[i].replaceAll(
                                            ".*\"amount\":(\\S+?)[,}].*", "$1");
                                    double price = Double.parseDouble(amount);
                                    final int index = i + 1;
                                    series.getData().add(new XYChart.Data<>(index, price));
                                } catch (Exception e) {
                                    System.err.println("Chart parse error: " + e.getMessage());
                                }
                            }

                            javafx.application.Platform.runLater(() -> {
                                bidChart.getData().clear();
                                bidChart.getData().add(series);
                            });
                        }
                    }
                }
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