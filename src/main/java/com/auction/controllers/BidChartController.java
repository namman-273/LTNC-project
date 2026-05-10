package com.auction.controllers;

import com.auction.model.BidTransaction;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
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
import java.util.function.Consumer;

public class BidChartController implements Initializable {

    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label titleLabel;

    private String auctionId, itemName, currentPrice, status, username;
    private long endTime;
    private Consumer<String> pushListener;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setData(String auctionId, String itemName, String currentPrice,
                        String status, String username, long endTime) {
        this.auctionId    = auctionId;
        this.itemName     = itemName;
        this.currentPrice = currentPrice;
        this.status       = status;
        this.username     = username;
        this.endTime      = endTime;
        titleLabel.setText("Biểu đồ giá - " + itemName);
        loadChartData();
        registerPushListener();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        xAxis.setLabel("Lần đặt giá");
        yAxis.setLabel("Giá (VND)");
    }

    private void loadChartData() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_GET_HISTORY + Protocol.SEPARATOR + auctionId
            );
            System.out.println("Chart history: " + response);

            if (response == null || !response.startsWith(Protocol.RES_HISTORY)) return;

            String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
            if (parts.length < 3 || parts[2].trim().equals("[]")) return;

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
        }).start();
    }

    private void registerPushListener() {
        pushListener = message -> {
            if (Protocol.isNotificationType(message, Protocol.NOTI_BID_UPDATE)) {
                String[] parts = message.split("\\" + Protocol.SEPARATOR);
                if (parts.length >= 3 && parts[1].equals(auctionId)) {
                    try {
                        double newPrice = Double.parseDouble(parts[2]);
                        Platform.runLater(() -> appendPoint(newPrice));
                    } catch (NumberFormatException ignored) {}
                }
            }
        };
        ServerConnection.getInstance().addPushListener(pushListener);
    }

    private void appendPoint(double price) {
        if (bidChart.getData().isEmpty()) return;
        XYChart.Series<Number, Number> series = bidChart.getData().get(0);
        int nextIndex = series.getData().size() + 1;
        series.getData().add(new XYChart.Data<>(nextIndex, price));
    }

    @FXML
    private void handleBack() {
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
        Stage stage = (Stage) bidChart.getScene().getWindow();
        new BidView(stage, auctionId, itemName, currentPrice, status, username, endTime).show();
    }
}