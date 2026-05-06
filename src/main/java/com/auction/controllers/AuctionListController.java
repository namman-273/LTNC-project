package com.auction.controllers;

import com.auction.dto.AuctionRow;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AdminDashboardView;
import com.auction.views.BidView;
import com.auction.views.CreateAuctionView;
import com.auction.views.LoginView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AuctionListController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Button adminButton;
    @FXML private Label statusBarLabel;

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

    private void loadFromServer() {
        setStatusBar("Đang tải danh sách phiên...");

        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                if (!conn.isConnected()) conn.connect();

                String response = conn.sendAndReceive("LIST_AUCTIONS");
                System.out.println("RAW: " + response);

                ObservableList<AuctionRow> data = FXCollections.observableArrayList();

                if (response != null && response.contains(Protocol.RES_LIST_SUCCESS)) {
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
                    setStatusBar("Tải xong " + finalData.size() + " phiên.");
                });

            } catch (Exception e) {
                System.err.println("Lỗi load danh sách: " + e.getMessage());
                Platform.runLater(() -> setStatusBar("Lỗi kết nối server!"));
            }
        }).start();
    }

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
            System.err.println("Lỗi parse JSON danh sách phiên: " + e.getMessage());
        }
        return result;
    }

    private void setStatusBar(String msg) {
        if (statusBarLabel != null) {
            statusBarLabel.setText(msg);
        }
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
        new BidView(stage, selected.getId(), selected.getItemName(),
                selected.getCurrentPrice(), selected.getStatus(), username).show();
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
}
