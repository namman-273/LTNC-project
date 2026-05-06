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
    private ServerConnection listenerConn;
    private Thread listenerThread;

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.username = username;
        titleLabel.setText("Biểu đồ giá - " + itemName);
        loadChartData();
        startListening();
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

                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Giá đặt");

                String[] tokens = json.split("\"amount\":");
                int index = 1;
                for (int i = 1; i < tokens.length; i++) {
                    try {
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

    private void startListening() {
        String pwd = SessionManager.getInstance().getPassword();
        if (pwd == null) return;

        listenerThread = new Thread(() -> {
            listenerConn = new ServerConnection("localhost", 9999);
            try {
                if (!listenerConn.connectDirect()) return;
                listenerConn.sendAndReceive("LOGIN|" + username + "|" + pwd);

                while (!Thread.currentThread().isInterrupted()) {
                    String message = listenerConn.receive();
                    if (message == null) break;

                    if (message.startsWith("UPDATE")) {
                        String[] parts = message.split("\\|");
                        if (parts.length >= 3 && parts[1].equals(auctionId)) {
                            double newPrice = Double.parseDouble(parts[2]);
                            Platform.runLater(() -> appendPoint(newPrice));
                        }
                    }
                }
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted())
                    System.err.println("Chart listener error: " + e.getMessage());
            } finally {
                if (listenerConn != null) listenerConn.disconnectDirect();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void appendPoint(double price) {
        if (bidChart.getData().isEmpty()) return;
        XYChart.Series<Number, Number> series = bidChart.getData().get(0);
        int nextIndex = series.getData().size() + 1;
        series.getData().add(new XYChart.Data<>(nextIndex, price));
    }

    @FXML
    private void handleBack() {
        if (listenerThread != null) listenerThread.interrupt();
        Stage stage = (Stage) bidChart.getScene().getWindow();
        new BidView(stage, auctionId, itemName, currentPrice, status, username).show();
    }
}