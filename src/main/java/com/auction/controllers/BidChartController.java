package com.auction.controllers;

import com.auction.model.BidTransaction;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.BidView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

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

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username) {
        this.auctionId    = auctionId;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.username     = username;
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

                // Dùng Protocol constants
                conn.sendAndReceive(
                        Protocol.CMD_LOGIN + Protocol.SEPARATOR + username + Protocol.SEPARATOR + pwd
                );
                String response = conn.sendAndReceive(
                        Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId
                );
                System.out.println("Chart history: " + response);

                if (response == null || !response.startsWith(Protocol.RES_HISTORY)) return;

                String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
                if (parts.length < 3 || parts[2].trim().equals("[]")) return;

                // Dùng Gson + BidTransaction model của BE thay vì tự parse string
                BidTransaction[] history = gson.fromJson(parts[2].trim(), BidTransaction[].class);
                if (history == null || history.length == 0) return;

                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Giá đặt");

                for (int i = 0; i < history.length; i++) {
                    final int idx = i + 1;
                    final double price = history[i].getAmount();
                    series.getData().add(new XYChart.Data<>(idx, price));
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

                // Dùng Protocol constants
                listenerConn.sendAndReceive(
                        Protocol.CMD_LOGIN + Protocol.SEPARATOR + username + Protocol.SEPARATOR + pwd
                );

                while (!Thread.currentThread().isInterrupted()) {
                    String message = listenerConn.receive();
                    if (message == null) break;

                    // Dùng Protocol.UPDATE thay vì hardcode
                    if (message.startsWith(Protocol.UPDATE)) {
                        String[] parts = message.split("\\" + Protocol.SEPARATOR);
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