package com.auction.controllers;

import com.auction.dto.AuctionRow;
import com.auction.network.Protocol;
import com.auction.util.AlertUtil;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.AdminDashboardView;
import com.auction.views.BalanceView;
import com.auction.views.BidView;
import com.auction.views.CreateAuctionView;
import com.auction.views.LoginView;
import com.auction.views.SellerView;
import com.auction.views.WatchlistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.auction.util.NotificationManager;
import com.auction.views.NotificationView;

public class AuctionListController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private TableView<AuctionRow> auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Button adminButton;
    @FXML private Button sellerButton;
    @FXML private Label statusBarLabel;

    private String username;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Xin chào, " + username + "!");
        String role = SessionManager.getInstance().getRole();
        if (adminButton != null) {
            adminButton.setVisible("ADMIN".equalsIgnoreCase(role));
            adminButton.setManaged("ADMIN".equalsIgnoreCase(role));
        }
        if (sellerButton != null) {
            sellerButton.setVisible("SELLER".equalsIgnoreCase(role));
            sellerButton.setManaged("SELLER".equalsIgnoreCase(role));
        }
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

        loadFromServer();
    }

    private void loadFromServer() {
        setStatusBar("Đang tải danh sách phiên...");

        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                if (!conn.isConnected()) {
                    if (!conn.connectWithRetry()) {
                        Platform.runLater(() -> {
                            setStatusBar("❌ Mất kết nối server! Nhấn 🔄 Làm mới để thử lại.");
                            AlertUtil.showError("Mất kết nối",
                                    "Không thể kết nối server!\nVui lòng kiểm tra server đang chạy rồi nhấn 🔄 Làm mới.");
                        });
                        return;
                    }
                }

                String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
                System.out.println("RAW: " + response);

                if (response == null || response.startsWith("ERROR|Mất kết nối")) {
                    Platform.runLater(() -> {
                        setStatusBar("❌ Mất kết nối server! Nhấn 🔄 Làm mới để thử lại.");
                        AlertUtil.showError("Mất kết nối",
                                "Mất kết nối khi tải danh sách phiên!\nNhấn 🔄 Làm mới để thử lại.");
                    });
                    return;
                }

                ObservableList<AuctionRow> data = FXCollections.observableArrayList();

                if (response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                    String json = response.substring(Protocol.RES_LIST_SUCCESS.length()
                            + Protocol.SEPARATOR.length());
                    AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                    if (rows != null) {
                        for (AuctionRow row : rows) {
                            if (!"FINISHED".equals(row.getStatus()) && !"PAID".equals(row.getStatus())) {
                                data.add(row);
                            }
                        }
                    };
                }

                if (data.isEmpty()) {
                    data.add(new AuctionRow("---", "Chưa có phiên nào", 0, "---", 0));
                }

                final ObservableList<AuctionRow> finalData = data;
                Platform.runLater(() -> {
                    auctionTable.setItems(finalData);
                    setStatusBar("✅ Tải xong " + finalData.size() + " phiên.");
                });

            } catch (Exception e) {
                System.err.println("Lỗi load danh sách: " + e.getMessage());
                Platform.runLater(() -> {
                    setStatusBar("❌ Mất kết nối server! Nhấn 🔄 Làm mới để thử lại.");
                    AlertUtil.showError("Lỗi tải danh sách",
                            "Đã xảy ra lỗi khi tải danh sách phiên!\nNhấn 🔄 Làm mới để thử lại.");
                });
            }
        }).start();
    }

    // ─── Watchlist ───────────────────────────────────────────────────────────

    @FXML
    private void handleWatch() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatusBar("⚠️ Vui lòng chọn một phiên!"); return; }

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_WATCH + Protocol.SEPARATOR + selected.getId()
            );
            Platform.runLater(() -> {
                if (response == null) {
                    setStatusBar("❌ Mất kết nối!");
                    AlertUtil.showError("Mất kết nối", "Mất kết nối khi theo dõi phiên!");
                    return;
                }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_WATCH_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : selected.getItemName();
                    setStatusBar("✅ Đã theo dõi phiên!");
                    AlertUtil.showSuccess("Theo dõi thành công",
                            "✅ Bạn đã theo dõi phiên:\n" + selected.getItemName()
                                    + "\nBạn sẽ nhận được thông báo realtime khi có bid mới!");
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Theo dõi thất bại!";
                    setStatusBar("❌ " + msg);
                }
            });
        }).start();
    }

    @FXML
    private void handleUnwatch() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatusBar("⚠️ Vui lòng chọn một phiên!"); return; }

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selected.getId()
            );
            Platform.runLater(() -> {
                if (response == null) {
                    setStatusBar("❌ Mất kết nối!");
                    AlertUtil.showError("Mất kết nối", "Mất kết nối khi bỏ theo dõi phiên!");
                    return;
                }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Đã bỏ theo dõi!";
                    setStatusBar("✅ " + msg);
                    AlertUtil.showSuccess("Bỏ theo dõi",
                            "❌ Đã bỏ theo dõi phiên:\n" + selected.getItemName());
                } else {
                    String msg = parts.length > 1 ? parts[1] : "Bỏ theo dõi thất bại!";
                    setStatusBar("❌ " + msg);
                }
            });
        }).start();
    }

    @FXML
    private void handleGetWatchlist() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new WatchlistView(stage, username).show();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private void setStatusBar(String msg) {
        if (statusBarLabel != null) statusBarLabel.setText(msg);
    }

    @FXML
    public void handleRefresh() { loadFromServer(); }

    public void refreshList() { loadFromServer(); }

    @FXML
    private void handleViewDetail() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatusBar("⚠️ Vui lòng chọn một phiên trước!");
            return;
        }
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new BidView(stage, selected.getId(), selected.getItemName(),
                String.valueOf(selected.getCurrentPrice()), selected.getStatus(),
                username, selected.getEndTime()).show();
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

    @FXML
    private void handleSellerDashboard() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new SellerView(stage, username).show();
    }

    @FXML
    private void handleBalance() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new BalanceView(stage, username).show();
    }
    @FXML
    private void handleNotification() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new NotificationView(stage, username).show();
    }
}