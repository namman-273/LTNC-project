package com.auction.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionListView {

    private Stage stage;
    private String username;

    public AuctionListView(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void show() {
        Label titleLabel = new Label("Xin chào, " + username + " | Danh sách phiên đấu giá");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TableView<AuctionRow> table = new TableView<>();

        TableColumn<AuctionRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);

        TableColumn<AuctionRow, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        nameCol.setPrefWidth(200);

        TableColumn<AuctionRow, String> priceCol = new TableColumn<>("Giá hiện tại");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        priceCol.setPrefWidth(120);

        TableColumn<AuctionRow, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(priceCol);
        table.getColumns().add(statusCol);

        ObservableList<AuctionRow> mockData = FXCollections.observableArrayList(
            new AuctionRow("A001", "Tranh Sơn Dầu", "5,000,000 VND", "OPEN"),
            new AuctionRow("A002", "Laptop Dell XPS", "15,000,000 VND", "OPEN"),
            new AuctionRow("A003", "Honda SH 2023", "80,000,000 VND", "FINISHED")
        );
        table.setItems(mockData);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.getChildren().add(titleLabel);
        layout.getChildren().add(table);

        Scene scene = new Scene(layout, 560, 400);
        stage.setTitle("Danh sách phiên - Auction System");
        stage.setScene(scene);
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

        public String getId() { return id; }
        public String getItemName() { return itemName; }
        public String getCurrentPrice() { return currentPrice; }
        public String getStatus() { return status; }
    }
}