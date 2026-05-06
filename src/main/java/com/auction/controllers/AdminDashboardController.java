package com.auction.controllers;

import com.auction.dto.AuctionRow;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AuctionListView;
import com.auction.views.CreateAuctionView;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label messageLabel;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // FIX: Thêm màu status giống AuctionListController cho nhất quán
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

        loadFromServer();
    }

    /**
     * FIX: Dùng ServerConnection.getInstance() thay vì tạo connection mới + login lại.
     * Admin đã login sẵn từ màn hình Login — connection chính vẫn còn hiệu lực.
     */
    public void loadFromServer() {
        showMessage("Đang tải danh sách...", "gray");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("LIST_AUCTIONS");

            ObservableList<AuctionRow> data = FXCollections.observableArrayList();

            if (response != null && response.contains("LIST_AUCTIONS_SUCCESS")) {
                int jsonStart = response.indexOf("[");
                if (jsonStart != -1) {
                    data = parseAuctionJson(response.substring(jsonStart));
                }
            }

            if (data.isEmpty()) {
                data.add(new AuctionRow("---", "Chưa có phiên nào", "---", "---"));
            }

            final ObservableList<AuctionRow> finalData = data;
            Platform.runLater(() -> {
                auctionTable.setItems(finalData);
                showMessage("Tải xong " + finalData.size() + " phiên.", "gray");
            });

        }).start();
    }

    /**
     * FIX: Parse JSON thay vì cắt chuỗi toString() thủ công.
     * Dùng chung logic với AuctionListController.
     */
    private ObservableList<AuctionRow> parseAuctionJson(String json) {
        ObservableList<AuctionRow> result = FXCollections.observableArrayList();
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj  = element.getAsJsonObject();
                String id       = obj.has("id")           ? obj.get("id").getAsString()           : "---";
                String itemName = obj.has("itemName")     ? obj.get("itemName").getAsString()     : "---";
                double priceRaw = obj.has("currentPrice") ? obj.get("currentPrice").getAsDouble() : 0;
                String status   = obj.has("status")       ? obj.get("status").getAsString()       : "---";

                result.add(new AuctionRow(id, itemName, String.format("%,.0f VND", priceRaw), status));
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON admin dashboard: " + e.getMessage());
        }
        return result;
    }

    @FXML
    private void handleRefresh() {
        loadFromServer();
    }

    /**
     * FIX: Dùng connection chính để END_AUCTION — không tạo connection mới.
     */
    @FXML
    private void handleEndAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên để kết thúc!", "red");
            return;
        }
        if ("FINISHED".equals(selected.getStatus())) {
            showMessage("Phiên này đã kết thúc rồi!", "red");
            return;
        }

        showMessage("Đang kết thúc phiên...", "orange");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("END_AUCTION|" + selected.getId());
            System.out.println("End auction: " + response);

            Platform.runLater(() -> {
                if (response != null && response.contains("SUCCESS")) {
                    showMessage("✅ Kết thúc phiên thành công!", "green");
                } else {
                    showMessage("❌ Lỗi: " + response, "red");
                }
                loadFromServer();
            });
        }).start();
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
