package com.auction.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.CreateAuctionView;
import com.auction.dto.AuctionRow;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * SellerController - Màn hình quản lý phiên đấu giá của Seller.
 * Chức năng:
 * - Xem danh sách phiên của mình
 * - Tạo phiên mới
 * - Xem lịch sử đặt giá của từng phiên
 */
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

    private ObservableList<AuctionRow> auctionData = FXCollections.observableArrayList();
    private ObservableList<String> historyData = FXCollections.observableArrayList();
    private String username;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        username = SessionManager.getInstance().getUsername();
        welcomeLabel.setText("Xin chào, " + username + "!");

        // Setup bảng
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Màu trạng thái
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

        // Khi chọn phiên → load lịch sử
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
                conn.sendAndReceive("LOGIN|" + username + "|" + pwd);
                String response = conn.sendAndReceive("LIST_AUCTIONS");

                if (response != null && response.contains("LIST_AUCTIONS_SUCCESS")) {
                    int start = response.indexOf("[");
                    if (start == -1) return;
                    ObservableList<AuctionRow> data = FXCollections.observableArrayList();
                    com.google.gson.JsonArray array = com.google.gson.JsonParser
                            .parseString(response.substring(start)).getAsJsonArray();

                    for (com.google.gson.JsonElement el : array) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        String id       = obj.has("id")           ? obj.get("id").getAsString()           : "---";
                        String itemName = obj.has("itemName")     ? obj.get("itemName").getAsString()     : "---";
                        double priceRaw = obj.has("currentPrice") ? obj.get("currentPrice").getAsDouble() : 0;
                        String status   = obj.has("status")       ? obj.get("status").getAsString()       : "---";
                        data.add(new AuctionRow(id, itemName, String.format("%,.0f VND", priceRaw), status));
                    }

                    long open = data.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
                    long finished = data.stream().filter(r -> "FINISHED".equals(r.getStatus())).count();

                    Platform.runLater(() -> {
                        auctionData.setAll(data);
                        statsLabel.setText("Tổng: " + data.size() + " phiên  |  Đang mở: " + open + "  |  Đã kết thúc: " + finished);
                    });
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
                conn.sendAndReceive("LOGIN|" + username + "|" + pwd);
                String response = conn.sendAndReceive("GET_HISTORY|" + auctionId);

                Platform.runLater(() -> {
                    historyData.clear();
                    if (response == null || !response.contains("HISTORY_RES")) {
                        historyData.add("Chưa có lịch sử đặt giá.");
                        return;
                    }

                    String[] parts = response.split("\\|", 3);
                    if (parts.length < 3 || parts[2].trim().equals("[]")) {
                        historyData.add("Chưa có lịch sử đặt giá.");
                        return;
                    }

                    String json = parts[2].trim();
                    String[] tokens = json.split("\"amount\":");
                    for (int i = 1; i < tokens.length; i++) {
                        try {
                            String numStr = tokens[i].split("[,}]")[0].trim();
                            double price = Double.parseDouble(numStr);

                            // Lấy username người đặt
                            String bidder = "---";
                            if (tokens[i].contains("\"username\":")) {
                                bidder = tokens[i].replaceAll(".*\"username\":\"([^\"]+)\".*", "$1");
                            }
                            historyData.add((i) + ". " + bidder + " đặt: " + String.format("%,.0f VND", price));
                        } catch (Exception e) {
                            System.err.println("Parse error: " + e.getMessage());
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

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }

}
