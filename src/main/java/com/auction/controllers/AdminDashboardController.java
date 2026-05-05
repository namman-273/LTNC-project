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
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadFromServer();
    }

    public void loadFromServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive("LIST_AUCTIONS");

            ObservableList<AuctionRow> data = FXCollections.observableArrayList();

            if (response != null && response.contains("LIST_AUCTIONS_SUCCESS")) {
                int start = response.indexOf("[");
                if (start != -1) {
                    String content = response.substring(start)
                            .replace("[", "").replace("]", "").trim();

                    String[] auctions = content.split(",\\s*(?=id=)");
                    for (String a : auctions) {
                        a = a.trim();
                        if (a.isEmpty() || !a.contains("id=")) {
                            continue;
                        }
                        String id = extractField(a, "id=");
                        String itemName = extractField(a, "itemName=");
                        String price = extractField(a, "currentPrice=");
                        String status = extractField(a, "status=");
                        try {
                            double p = Double.parseDouble(price);
                            price = String.format("%,.0f VND", p);
                        } catch (NumberFormatException ignored) {
                            // ignored
                        }
                        data.add(new AuctionRow(id, itemName, price, status));
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
        if (start == -1) {
            return "---";
        }
        start += key.length();
        int end = text.length();
        String[] nextKeys = {"id=", "itemName=", "currentPrice=", "status="};
        for (String nextKey : nextKeys) {
            int pos = text.indexOf("," + nextKey, start);
            if (pos != -1 && pos < end) {
                end = pos;
            }
        }
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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                // ignored
            }
            Platform.runLater(() -> {
                if (response != null && response.contains("SUCCESS")) {
                    showMessage("✅ Kết thúc phiên thành công!", "green");
                } else {
                    showMessage("❌ Lỗi: " + response, "red");
                }
            });
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                // ignored
            }
            Platform.runLater(() -> loadFromServer());
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

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }

    public static class AuctionRow {
        private String id;
        private String itemName;
        private String currentPrice;
        private String status;

        public AuctionRow(String id, String itemName, String currentPrice, String status) {
            this.id = id;
            this.itemName = itemName;
            this.currentPrice = currentPrice;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getItemName() {
            return itemName;
        }

        public String getCurrentPrice() {
            return currentPrice;
        }

        public String getStatus() {
            return status;
        }
    }
}
