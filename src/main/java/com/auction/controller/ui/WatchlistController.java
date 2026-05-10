package com.auction.controller.ui;

import com.auction.dto.AuctionRow;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.BidView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.net.URL;
import java.util.ResourceBundle;

public class WatchlistController implements Initializable {

    @FXML private TableView<AuctionRow> watchlistTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label messageLabel;

    private String username;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setUsername(String username) {
        this.username = username;
        loadWatchlist();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
    }

    private void loadWatchlist() {
        showMessage("Đang tải watchlist...", "gray");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_GET_WATCHLIST
            String response = conn.sendAndReceive(Protocol.CMD_GET_WATCHLIST);
            System.out.println("Watchlist response: " + response);

            Platform.runLater(() -> {
                if (response == null) {
                    showMessage("Mất kết nối server!", "red");
                    return;
                }

                ObservableList<AuctionRow> data = FXCollections.observableArrayList();

                if (response.startsWith(Protocol.RES_WATCHLIST)) {
                    // Dùng Gson deserialize thẳng vào AuctionRow[]
                    String json = response.substring(Protocol.RES_WATCHLIST.length()
                            + Protocol.SEPARATOR.length());
                    AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                    if (rows != null) data.addAll(rows);
                } else {
                    String[] parts = response.split("\\" + Protocol.SEPARATOR);
                    String msg = parts.length > 1 ? parts[1] : "Lỗi tải watchlist!";
                    showMessage("❌ " + msg, "red");
                    return;
                }

                if (data.isEmpty()) {
                    showMessage("Chưa có phiên nào trong watchlist.", "gray");
                } else {
                    showMessage("✅ " + data.size() + " phiên đang theo dõi.", "green");
                }
                watchlistTable.setItems(data);
            });
        }).start();
    }

    @FXML
    private void handleRefresh() { loadWatchlist(); }

    @FXML
    private void handleUnwatch() {
        AuctionRow selected = watchlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên!", "red");
            return;
        }

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selected.getId()
            );
            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Đã bỏ theo dõi!";
                    showMessage("✅ " + msg, "green");
                    loadWatchlist();
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Bỏ theo dõi thất bại!";
                    showMessage("❌ " + msg, "red");
                }
            });
        }).start();
    }

    @FXML
    private void handleViewDetail() {
        AuctionRow selected = watchlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên!", "red");
            return;
        }
        Stage stage = (Stage) watchlistTable.getScene().getWindow();
        new BidView(stage, selected.getId(), selected.getItemName(),
                String.valueOf(selected.getCurrentPrice()), selected.getStatus(),
                username, selected.getEndTime()).show();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) watchlistTable.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            messageLabel.setText(msg);
        }
    }
}
