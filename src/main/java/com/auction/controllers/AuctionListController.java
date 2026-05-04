package com.auction.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.LoginView;
import com.auction.views.BidView;
import com.auction.views.CreateAuctionView;
import com.auction.views.AdminDashboardView;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Button adminButton;

    private String username;

    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Xin chào, " + username + "!");
        String role = SessionManager.getInstance().getRole();
        if (adminButton != null) {
            adminButton.setVisible("ADMIN".equalsIgnoreCase(role));
            adminButton.setManaged("ADMIN".equalsIgnoreCase(role));
        }
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
        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                if (!conn.isConnected()) {
                    conn.connect();
                }
                String response = conn.sendAndReceive("LIST_AUCTIONS");
                System.out.println("RAW: " + response);

                ObservableList<AuctionRow> data = FXCollections.observableArrayList();

                if (response != null && response.contains("LIST_AUCTIONS_SUCCESS")) {
                    int start = response.indexOf("[");
                    if (start != -1) {
                        String content = response.substring(start)
                                .replace("[", "").replace("]", "").trim();
                        String[] auctions = content.split(",\\s*(?=id=)");
                        for (String a : auctions) {
                            a = a.trim();
                            if (a.isEmpty() || !a.contains("id=")) continue;
                            String id       = extractField(a, "id=");
                            String itemName = extractField(a, "itemName=");
                            String price    = extractField(a, "currentPrice=");
                            String status   = extractField(a, "status=");
                            try {
                                double p = Double.parseDouble(price);
                                price = String.format("%,.0f VND", p);
                            } catch (NumberFormatException ignored) {}
                            data.add(new AuctionRow(id, itemName, price, status));
                        }
                    }
                }

                if (data.isEmpty()) {
                    data.add(new AuctionRow("---", "Chưa có phiên nào", "---", "---"));
                }

                Platform.runLater(() -> auctionTable.setItems(data));

            } catch (Exception e) {
                System.err.println("Lỗi load danh sách: " + e.getMessage());
            }
        }).start();
    }

    private String extractField(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) return "---";
        start += key.length();
        int end = text.length();
        String[] nextKeys = {"id=", "itemName=", "currentPrice=", "status="};
        for (String nextKey : nextKeys) {
            int pos = text.indexOf("," + nextKey, start);
            if (pos != -1 && pos < end) end = pos;
        }
        return text.substring(start, end).trim();
    }

    @FXML
    public void handleRefresh() {
        loadFromServer();
    }

    public void refreshList() {
        loadFromServer();
    }

    @FXML
    private void handleViewDetail() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Chưa chọn phiên nào!");
            return;
        }
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        BidView bidView = new BidView(stage, selected.getId(), selected.getItemName(),
                selected.getCurrentPrice(), selected.getStatus(), username);
        bidView.show();
    }

    @FXML
    private void handleLogout() {
        ServerConnection.getInstance().disconnect();
        SessionManager.getInstance().clear();
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new LoginView(stage).show();
    }

    @FXML
    private void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void handleAdminDashboard() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new AdminDashboardView(stage, username).show();
    }

    public static class AuctionRow {
        private String id, itemName, currentPrice, status;

        public AuctionRow(String id, String itemName, String currentPrice, String status) {
            this.id = id; this.itemName = itemName;
            this.currentPrice = currentPrice; this.status = status;
        }

        public String getId() { return id; }
        public String getItemName() { return itemName; }
        public String getCurrentPrice() { return currentPrice; }
        public String getStatus() { return status; }
    }
}
