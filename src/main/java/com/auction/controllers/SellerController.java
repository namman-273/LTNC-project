package com.auction.controllers;

import com.auction.dto.AuctionRow;
import com.auction.model.BidTransaction;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.CreateAuctionView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label statsLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private ListView<String> historyList;
    @FXML private Label historyTitleLabel;
    @FXML private Label messageLabel;

    private final ObservableList<AuctionRow> auctionData = FXCollections.observableArrayList();
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private String username;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Lấy username từ SessionManager của BE
        username = SessionManager.getInstance().getUsername();
        welcomeLabel.setText("Xin chào, " + username + "!");

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "OPEN":     setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;"); break;
                    case "RUNNING":  setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;"); break;
                    case "FINISHED": setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;"); break;
                    default:         setStyle("-fx-text-fill: #888888;");
                }
            }
        });

        auctionTable.setItems(auctionData);
        historyList.setItems(historyData);

        auctionTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) loadHistory(newVal.getId(), newVal.getItemName());
                }
        );

        loadMyAuctions();
    }

    private void loadMyAuctions() {
        statsLabel.setText("Đang tải...");
        new Thread(() -> {
            ServerConnection conn = new ServerConnection("localhost", 9999);
            try {
                String pwd = SessionManager.getInstance().getPassword();
                if (!conn.connectDirect()) return;

                // Dùng Protocol constants build request
                conn.sendAndReceive(
                        Protocol.CMD_LOGIN + Protocol.SEPARATOR + username + Protocol.SEPARATOR + pwd
                );
                String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);

                if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                    // Dùng Gson deserialize thẳng vào AuctionRow[]
                    String json = response.substring(Protocol.RES_LIST_SUCCESS.length()
                            + Protocol.SEPARATOR.length());
                    AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);

                    if (rows != null) {
                        ObservableList<AuctionRow> data = FXCollections.observableArrayList(rows);
                        long open     = data.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
                        long finished = data.stream().filter(r -> "FINISHED".equals(r.getStatus())).count();

                        Platform.runLater(() -> {
                            auctionData.setAll(data);
                            statsLabel.setText("Tổng: " + data.size() + " phiên  |  Đang mở: "
                                    + open + "  |  Đã kết thúc: " + finished);
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> statsLabel.setText("Lỗi tải dữ liệu"));
            } finally {
                conn.disconnectDirect();
            }
        }).start();
    }

    private void loadHistory(String auctionId, String itemName) {
        historyTitleLabel.setText("📋 Lịch sử đặt giá - " + itemName);
        historyData.clear();
        historyData.add("Đang tải...");

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

                Platform.runLater(() -> {
                    historyData.clear();
                    if (response == null || !response.startsWith(Protocol.RES_HISTORY)) {
                        historyData.add("Chưa có lịch sử đặt giá.");
                        return;
                    }

                    String[] parts = response.split("\\" + Protocol.SEPARATOR, 3);
                    if (parts.length < 3 || parts[2].trim().equals("[]")) {
                        historyData.add("Chưa có lịch sử đặt giá.");
                        return;
                    }

                    // Dùng Gson + BidTransaction model của BE thay vì tự parse
                    BidTransaction[] history = gson.fromJson(parts[2].trim(), BidTransaction[].class);
                    if (history != null) {
                        for (int i = 0; i < history.length; i++) {
                            BidTransaction bt = history[i];
                            String bidder = bt.getBidder() != null
                                    ? bt.getBidder().getUsername() : "---";
                            historyData.add((i + 1) + ". " + bidder + " đặt: "
                                    + String.format("%,.0f VND", bt.getAmount()));
                        }
                    }
                    if (historyData.isEmpty()) historyData.add("Chưa có lịch sử đặt giá.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> historyData.add("Lỗi tải lịch sử."));
            } finally {
                conn.disconnectDirect();
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        historyData.clear();
        historyTitleLabel.setText("📋 Lịch sử đặt giá");
        loadMyAuctions();
    }

    @FXML
    private void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }
}