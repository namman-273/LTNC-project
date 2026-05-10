package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.Protocol;
import com.auction.util.AlertUtil;
import com.auction.util.ServerConnection;
import com.auction.util.SessionManager;
import com.auction.views.java.AdminDashboardView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.BidView;
import com.auction.views.java.CreateAuctionView;
import com.auction.views.java.LoginView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.SellerView;
import com.auction.views.java.WatchlistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionListController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private FlowPane auctionGrid;
    @FXML private Button adminButton;
    @FXML private Button sellerButton;
    @FXML private Label statusBarLabel;

    private String username;
    private AuctionRow selectedRow = null;
    private final List<AuctionRow> currentRows = new ArrayList<>();

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
                            setStatusBar("❌ Mất kết nối server!");
                            AlertUtil.showError("Mất kết nối", "Không thể kết nối server!");
                        });
                        return;
                    }
                }
                String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
                if (response == null || response.startsWith("ERROR")) {
                    Platform.runLater(() -> setStatusBar("❌ Mất kết nối server!"));
                    return;
                }
                List<AuctionRow> data = new ArrayList<>();
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
                    }
                }
                final List<AuctionRow> finalData = data;
                Platform.runLater(() -> {
                    currentRows.clear();
                    currentRows.addAll(finalData);
                    renderCards(finalData);
                    setStatusBar(finalData.isEmpty()
                            ? "ℹ️ Chưa có phiên nào đang diễn ra."
                            : "✅ Tải xong " + finalData.size() + " phiên.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatusBar("❌ Lỗi tải danh sách!"));
            }
        }).start();
    }

    // ─── Card rendering ───────────────────────────────────────────────────────

    private void renderCards(List<AuctionRow> rows) {
        auctionGrid.getChildren().clear();
        selectedRow = null;
        if (rows.isEmpty()) {
            Label empty = new Label("Chưa có phiên nào. Nhấn 🔄 Làm mới để tải.");
            empty.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px; -fx-padding: 40;");
            auctionGrid.getChildren().add(empty);
            return;
        }
        for (AuctionRow row : rows) {
            auctionGrid.getChildren().add(buildCard(row));
        }
    }

    private VBox buildCard(AuctionRow row) {
        // Badge trạng thái
        String statusColor = switch (row.getStatus()) {
            case "RUNNING" -> "#E65100";
            case "OPEN"    -> "#2E7D32";
            default        -> "#888888";
        };
        Label badge = new Label("RUNNING".equals(row.getStatus()) ? "🔴 LIVE" : "⬤ " + row.getStatus());
        badge.setStyle(
                "-fx-background-color: " + statusColor + "22;" +
                        "-fx-text-fill: " + statusColor + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 3 8;");

        // Icon
        Label icon = new Label("🏷");
        icon.setStyle("-fx-font-size: 46px; -fx-padding: 8 0;");
        icon.setMinWidth(200);
        icon.setAlignment(Pos.CENTER);

        // Tên sản phẩm
        Label name = new Label(row.getItemName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-wrap-text: true;");
        name.setMaxWidth(185);

        // Giá
        Label priceLabel = new Label("Giá hiện tại");
        priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        Label price = new Label(row.getCurrentPriceFormatted());
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");

        // Nút xem chi tiết
        Button btnDetail = new Button("👁 Xem chi tiết");
        btnDetail.setPrefWidth(185);
        btnDetail.setStyle(
                "-fx-background-color: #1565C0; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 0;");
        btnDetail.setOnAction(e -> openBidView(row));
        btnDetail.setOnMouseEntered(e -> btnDetail.setStyle(btnDetail.getStyle()
                .replace("#1565C0; -fx-text-fill", "#0D47A1; -fx-text-fill")));
        btnDetail.setOnMouseExited(e -> btnDetail.setStyle(btnDetail.getStyle()
                .replace("#0D47A1; -fx-text-fill", "#1565C0; -fx-text-fill")));

        // Card container
        VBox card = new VBox(8, badge, icon, name, priceLabel, price, btnDetail);
        card.setPrefWidth(215);
        card.setMinHeight(265);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #EEF2FF; -fx-border-radius: 14; -fx-border-width: 1;" +
                        "-fx-padding: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(21,101,192,0.08), 10, 0, 0, 3);" +
                        "-fx-cursor: hand;");

        // Click để select
        card.setOnMouseClicked(e -> {
            auctionGrid.getChildren().forEach(n -> {
                if (n instanceof VBox v) {
                    v.setStyle(v.getStyle()
                            .replace("-fx-background-color: #EEF5FF;", "-fx-background-color: white;"));
                }
            });
            card.setStyle(card.getStyle()
                    .replace("-fx-background-color: white;", "-fx-background-color: #EEF5FF;"));
            selectedRow = row;
        });

        return card;
    }

    private void openBidView(AuctionRow row) {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new BidView(stage, row.getId(), row.getItemName(),
                String.valueOf(row.getCurrentPrice()), row.getStatus(),
                username, row.getEndTime(),
                row.getImageUrl(), row.getDescription()).show();
    }

    // ─── Watchlist ────────────────────────────────────────────────────────────

    @FXML
    private void handleWatch() {
        if (selectedRow == null) { setStatusBar("⚠️ Vui lòng click vào một phiên trước!"); return; }
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_WATCH + Protocol.SEPARATOR + selectedRow.getId());
            Platform.runLater(() -> {
                if (response != null && response.startsWith(Protocol.RES_WATCH_SUCCESS)) {
                    setStatusBar("✅ Đã theo dõi phiên!");
                    AlertUtil.showSuccess("Theo dõi thành công", "✅ Đang theo dõi: " + selectedRow.getItemName());
                } else {
                    setStatusBar("❌ Theo dõi thất bại!");
                }
            });
        }).start();
    }

    @FXML
    private void handleUnwatch() {
        if (selectedRow == null) { setStatusBar("⚠️ Vui lòng click vào một phiên trước!"); return; }
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selectedRow.getId());
            Platform.runLater(() -> {
                if (response != null && response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
                    setStatusBar("✅ Đã bỏ theo dõi!");
                } else {
                    setStatusBar("❌ Bỏ theo dõi thất bại!");
                }
            });
        }).start();
    }

    @FXML
    private void handleGetWatchlist() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new WatchlistView(stage, username).show();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    private void setStatusBar(String msg) {
        if (statusBarLabel != null) statusBarLabel.setText(msg);
    }

    @FXML public void handleRefresh() { loadFromServer(); }
    public  void refreshList()        { loadFromServer(); }

    @FXML
    private void handleLogout() {
        ServerConnection.getInstance().disconnect();
        SessionManager.getInstance().clear();
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new LoginView(stage).show();
    }

    @FXML
    private void handleCreateAuction() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void handleAdminDashboard() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new AdminDashboardView(stage, username).show();
    }

    @FXML
    private void handleSellerDashboard() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new SellerView(stage, username).show();
    }

    @FXML
    private void handleBalance() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new BalanceView(stage, username).show();
    }

    @FXML
    private void handleNotification() {
        Stage stage = (Stage) auctionGrid.getScene().getWindow();
        new NotificationView(stage, username).show();
    }
}
