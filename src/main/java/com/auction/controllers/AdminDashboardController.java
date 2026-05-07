package com.auction.controllers;

import com.auction.dto.AuctionRow;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;
import com.auction.views.CreateAuctionView;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label messageLabel;
    @FXML private Label balanceLabel;
    @FXML private TextField depositAmountField;
    @FXML private TextField depositUsernameField;

    private String username;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setUsername(String username) {
        this.username = username;
        loadBalance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));

        // Fix: thêm setCellValueFactory cho statusCol
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

        loadFromServer();
    }

    public void loadFromServer() {
        showMessage("Đang tải danh sách...", "gray");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_LIST_AUCTIONS
            String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);

            ObservableList<AuctionRow> data = FXCollections.observableArrayList();

            if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                // Dùng Gson deserialize thẳng vào AuctionRow[]
                String json = response.substring(Protocol.RES_LIST_SUCCESS.length()
                        + Protocol.SEPARATOR.length());
                AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                if (rows != null) data.addAll(rows);
            }

            if (data.isEmpty()) {
                data.add(new AuctionRow("---", "Chưa có phiên nào", 0, "---", 0));
            }

            final ObservableList<AuctionRow> finalData = data;
            Platform.runLater(() -> {
                auctionTable.setItems(finalData);
                showMessage("Tải xong " + finalData.size() + " phiên.", "gray");
            });
        }).start();
    }

    // ─── Deposit / Balance ───────────────────────────────────────────────────

    @FXML
    private void handleDeposit() {
        // Không tự validate — gửi thẳng lên BE
        // BE expect: DEPOSIT|targetUsername|amount
        String targetUser = depositUsernameField != null
                ? depositUsernameField.getText().trim() : username;
        String amount = depositAmountField != null
                ? depositAmountField.getText().trim() : "";

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_DEPOSIT + Protocol.SEPARATOR
                            + targetUser + Protocol.SEPARATOR
                            + amount
            );
            System.out.println("Deposit response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Nạp tiền thành công!";
                    showMessage("✅ " + msg, "green");
                    if (depositAmountField != null) depositAmountField.clear();
                    loadBalance();
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Nạp tiền thất bại!";
                    showMessage("❌ " + errorMsg, "red");
                }
            });
        }).start();
    }

    private void loadBalance() {
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(Protocol.CMD_GET_BALANCE);
            System.out.println("Balance response: " + response);

            Platform.runLater(() -> {
                if (response == null) return;
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BALANCE_INFO) && parts.length > 1) {
                    // Lấy balance từ BE trả về
                    if (balanceLabel != null) {
                        balanceLabel.setText("Số dư: " + String.format("%,.0f VND",
                                Double.parseDouble(parts[1])));
                    }
                }
            });
        }).start();
    }

    // ─── Auction Management ──────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadFromServer(); }

    @FXML
    private void handleEndAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên để kết thúc!", "red");
            return;
        }

        showMessage("Đang kết thúc phiên...", "orange");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            // Dùng Protocol.CMD_END_AUCTION
            String response = conn.sendAndReceive(
                    Protocol.CMD_END_AUCTION + Protocol.SEPARATOR + selected.getId()
            );
            System.out.println("End auction: " + response);

            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_END_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Kết thúc phiên thành công!";
                    showMessage("✅ " + msg, "green");
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Lỗi kết thúc phiên!";
                    showMessage("❌ " + errorMsg, "red");
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
