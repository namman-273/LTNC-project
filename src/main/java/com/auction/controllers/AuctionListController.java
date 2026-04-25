package com.auction.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.auction.views.LoginView;


import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;

    private String username;

    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Xin chào, " + username + "!");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Gắn cột với thuộc tính
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Load mock data (tuần sau thay bằng data thật từ server)
        loadMockData();
    }

    private void loadMockData() {
        ObservableList<AuctionRow> data = FXCollections.observableArrayList(
            new AuctionRow("A001", "Tranh Sơn Dầu", "5,000,000 VND", "OPEN"),
            new AuctionRow("A002", "Laptop Dell XPS", "15,000,000 VND", "OPEN"),
            new AuctionRow("A003", "Honda SH 2023", "80,000,000 VND", "FINISHED")
        );
        auctionTable.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        loadMockData();
    }

    @FXML
    private void handleViewDetail() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Chưa chọn phiên nào!");
            return;
        }
        System.out.println("Xem chi tiết: " + selected.getItemName());
        // Tuần sau sẽ mở màn hình chi tiết
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        LoginView loginView = new LoginView(stage);
        loginView.show();
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