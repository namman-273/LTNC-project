package com.auction.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
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

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadFromServer();
    }

    private void loadFromServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("LIST_AUCTIONS");

            ObservableList<AuctionRow> data = FXCollections.observableArrayList();

            if (response != null && response.contains("LIST_AUCTIONS_SUCCESS")) {
                String content = response.substring(response.indexOf("[") + 1, response.lastIndexOf("]"));

                if (!content.isEmpty()) {
                    String[] auctions = content.split("(?=id=)");
                    for (String a : auctions) {
                        a = a.replaceAll(",\\s*$", "").trim();
                        if (a.isEmpty()) continue;
                        String id = extractField(a, "id=");
                        String itemName = extractField(a, "itemName=");
                        String price = extractField(a, "currentPrice=");
                        String status = extractField(a, "status=");
                        try {
                            String formattedPrice = String.format("%,.0f VND", Double.parseDouble(price));
                            data.add(new AuctionRow(id, itemName, formattedPrice, status));
                        } catch (NumberFormatException e) {
                            data.add(new AuctionRow(id, itemName, price, status));
                        }
                    }
                }
            }

            if (data.isEmpty()) {
                data.add(new AuctionRow("---", "Chưa có phiên nào", "---", "---"));
            }

            auctionTable.setItems(data);

        } catch (Exception e) {
            System.err.println("Lỗi load danh sách: " + e.getMessage());
        }
    }

    private String extractField(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) return "---";
        start += key.length();
        int end = text.indexOf(",", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    @FXML
    private void handleRefresh() {
        loadFromServer();
    }

    @FXML
    private void handleEndAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Chưa chọn phiên nào!");
            return;
        }

        new Thread(() -> {
            ServerConnection conn = new ServerConnection("localhost", 9999);
            conn.connectDirect();
            conn.sendAndReceive("LOGIN|admin|admin123");
            String response = conn.sendAndReceive("END_AUCTION|" + selected.getId());
            conn.disconnect();
            System.out.println("End auction: " + response);

            Platform.runLater(() -> {
                loadFromServer();
            });
        }).start();
    }

    @FXML
    private void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        CreateAuctionView createView = new CreateAuctionView(stage, username);
        createView.show();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        AuctionListView listView = new AuctionListView(stage, username);
        listView.show();
    }

    public static class AuctionRow {
        private String id, itemName, currentPrice, status;

        public AuctionRow(String id, String itemName, String currentPrice, String status) {
            this.id = id;
            this.itemName = itemName;
            this.currentPrice = currentPrice;
            this.status = status;
        }

        public String getId() { return id; }
        public String getItemName() { return itemName; }
        public String getCurrentPrice() { return currentPrice; }
        public String getStatus() { return status; }
    }
}