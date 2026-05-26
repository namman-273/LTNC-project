package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.protocol.Protocol;
import com.auction.network.client.ServerConnection;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.CreateAuctionView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.ToastManager;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AdminDashboardController implements Initializable {

    @FXML private TableView<AuctionRow>           auctionTable;
    @FXML private TableColumn<AuctionRow, String> idCol;
    @FXML private TableColumn<AuctionRow, String> nameCol;
    @FXML private TableColumn<AuctionRow, String> priceCol;
    @FXML private TableColumn<AuctionRow, String> statusCol;
    @FXML private Label     messageLabel;
    @FXML private Label     balanceLabel;
    @FXML private Label     statTotalLabel;
    @FXML private Label     statOpenLabel;
    @FXML private Label     statFinishedLabel;
    @FXML private TextField depositAmountField;
    @FXML private TextField depositUsernameField;

    private String   username;
    private Timeline autoRefreshTimeline;

    // FIX: Giữ reference để có thể removePushListener khi thoát màn hình
    private Consumer<String> pushListener;

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
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "OPEN"     -> setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    case "RUNNING"  -> setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
                    case "FINISHED" -> setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    default         -> setStyle("-fx-text-fill: #888888;");
                }
            }
        });

        loadFromServer();
        startAutoRefresh();
        registerPushListener(); // FIX: đăng ký nhận broadcast từ server
        initToastManager(auctionTable);
    }

    private void initToastManager(javafx.scene.Node anchor) {
        Platform.runLater(() -> {
            try {
                javafx.scene.Parent root = anchor.getScene().getRoot();
                if (root instanceof StackPane) {
                    ToastManager.init((StackPane) root);
                } else {
                    javafx.scene.Scene scene = anchor.getScene();
                    StackPane overlay = new StackPane();
                    overlay.getChildren().add(root);
                    scene.setRoot(overlay);
                    ToastManager.init(overlay);
                }
            } catch (Exception e) {
                System.err.println("[AdminToast] Init failed: " + e.getMessage());
            }
        });
    }

    // FIX 2 — AdminDashboardController.java (xóa dead code, thêm NOTI_AUCTION_CANCELLED)
    private void registerPushListener() {
        pushListener = message -> {
            String[] parts = message.split("\\|");
            String header  = parts[0];
            switch (header) {
                // ĐÃ XÓA case RES_ADMIN_END_SUCCESS:
                // RES_ADMIN_END_SUCCESS KHÔNG phải push → nó vào responseQueue
                // và được handleEndAuction() xử lý qua sendAndReceive bình thường.
                // Giữ case này ở đây sẽ KHÔNG BAO GIỜ fire và gây nhầm lẫn.

                case Protocol.NOTI_AUCTION_CANCELLED -> {
                    // Format: AUCTION_CANCELLED|auctionId|reason
                    String cancelledId = parts.length >= 2 ? parts[1] : "";
                    String detail      = parts.length >= 3 ? parts[2] : "Phiên đã bị hủy";
                    Platform.runLater(() -> {
                        // Xóa ngay khỏi table mà không cần round-trip server
                        if (!cancelledId.isEmpty() && auctionTable.getItems() != null) {
                            auctionTable.getItems().removeIf(r -> cancelledId.equals(r.getId()));
                            // Cập nhật stat labels
                            long openCount     = auctionTable.getItems().stream()
                                    .filter(r -> "OPEN".equals(r.getStatus()) || "RUNNING".equals(r.getStatus())).count();
                            long finishedCount = auctionTable.getItems().stream()
                                    .filter(r -> "FINISHED".equals(r.getStatus()) || "PAID".equals(r.getStatus())).count();
                            if (statTotalLabel    != null) statTotalLabel.setText(String.valueOf(auctionTable.getItems().size()));
                            if (statOpenLabel     != null) statOpenLabel.setText(String.valueOf(openCount));
                            if (statFinishedLabel != null) statFinishedLabel.setText(String.valueOf(finishedCount));
                        }
                        showMessage("🚫 Phiên " + cancelledId + " bị hủy: " + detail, "gray");
                    });
                }
                default -> {}
            }
        };
        ServerConnection.getInstance().addPushListener(pushListener);
    }

    public void loadFromServer() {
        showMessage("Đang tải danh sách...", "gray");

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);

            ObservableList<AuctionRow> data = FXCollections.observableArrayList();

            if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
                String json = response.substring(
                        Protocol.RES_LIST_SUCCESS.length() + Protocol.SEPARATOR.length());
                AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
                if (rows != null) data.addAll(rows);
            }

            final ObservableList<AuctionRow> finalData = data;
            Platform.runLater(() -> {
                auctionTable.setItems(finalData);
                long openCount     = finalData.stream().filter(r -> "OPEN".equals(r.getStatus()) || "RUNNING".equals(r.getStatus())).count();
                long finishedCount = finalData.stream().filter(r -> "FINISHED".equals(r.getStatus()) || "PAID".equals(r.getStatus())).count();
                if (statTotalLabel    != null) statTotalLabel.setText(String.valueOf(finalData.size()));
                if (statOpenLabel     != null) statOpenLabel.setText(String.valueOf(openCount));
                if (statFinishedLabel != null) statFinishedLabel.setText(String.valueOf(finishedCount));
                showMessage(finalData.isEmpty()
                        ? "ℹ️ Chưa có phiên nào."
                        : "✅ Tải xong " + finalData.size() + " phiên.", "gray");
            });
        }).start();
    }

    @FXML
    public void handleDeposit() {
        String amount = depositAmountField != null
                ? depositAmountField.getText().trim() : "";

        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_DEPOSIT + Protocol.SEPARATOR + amount);
            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
                    showMessage("✅ " + (parts.length > 2 ? parts[2] : "Nạp tiền thành công!"), "green");
                    if (depositAmountField != null) depositAmountField.clear();
                    loadBalance();
                } else {
                    showMessage("❌ " + (parts.length > 1 ? parts[1] : "Nạp tiền thất bại!"), "red");
                }
            });
        }).start();
    }

    private void loadBalance() {
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_GET_BALANCE);
            Platform.runLater(() -> {
                if (response == null) return;
                String[] parts = response.split("\\" + Protocol.SEPARATOR);
                if (response.startsWith(Protocol.RES_BALANCE_INFO) && parts.length > 1) {
                    try {
                        if (balanceLabel != null)
                            balanceLabel.setText("Số dư: " + String.format("%,.0f VNĐ",
                                    Double.parseDouble(parts[1])));
                    } catch (NumberFormatException ignored) {}
                }
            });
        }).start();
    }

    @FXML public void handleRefresh() { loadFromServer(); }

    @FXML
    public void handleEndAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên để kết thúc!", "red"); return;
        }
        final String endAuctionId   = selected.getId();
        final String endAuctionName = selected.getItemName();
        showMessage("Đang kết thúc phiên...", "orange");
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_END_AUCTION + Protocol.SEPARATOR + endAuctionId);
            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                // RES_ADMIN_END_SUCCESS là direct response (1-1), KHÔNG phải push
                // → nó đi qua responseQueue → sendAndReceive nhận đúng, không timeout
                if (response.startsWith(Protocol.RES_ADMIN_END_SUCCESS)) {
                    showMessage("✅ Đã đóng phiên " + endAuctionName, "green");
                    NotificationManager.getInstance().add(
                            "⚠️ [Admin] Đã đóng sớm phiên: " + endAuctionName + " (" + endAuctionId + ")",
                            "system", endAuctionId);
                    ToastManager.show(ToastManager.Type.SUCCESS, "✅ Đã đóng phiên " + endAuctionName);
                } else {
                    String[] parts = response.split("\\" + Protocol.SEPARATOR);
                    String msg = parts.length > 1 ? parts[1] : "Lỗi kết thúc phiên!";
                    showMessage("❌ " + msg, "red");
                    ToastManager.show(ToastManager.Type.DANGER, "❌ " + msg);
                }
                loadFromServer();
            });
        }).start();
    }

    @FXML
    public void handleDeleteAuction() {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Vui lòng chọn một phiên để xóa!", "red"); return;
        }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa phiên:\n"
                + selected.getItemName() + "?\nHành động này không thể hoàn tác!");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) return;

        final String delAuctionId   = selected.getId();
        final String delAuctionName = selected.getItemName();
        showMessage("Đang xóa phiên...", "orange");
        new Thread(() -> {
            String response = ServerConnection.getInstance().sendAndReceive(
                    Protocol.CMD_DELETE_AUCTION + Protocol.SEPARATOR + delAuctionId);
            Platform.runLater(() -> {
                if (response == null) { showMessage("Mất kết nối server!", "red"); return; }
                if (response.startsWith(Protocol.RES_DELETE_SUCCESS)) {
                    showMessage("✅ Đã xóa phiên " + delAuctionName, "green");
                    NotificationManager.getInstance().add(
                            "🗑️ [Admin] Đã xóa phiên: " + delAuctionName + " (" + delAuctionId + ")",
                            "system", delAuctionId);
                    ToastManager.show(ToastManager.Type.SUCCESS, "🗑️ Đã xóa phiên " + delAuctionName);
                } else {
                    String[] parts = response.split("\\" + Protocol.SEPARATOR);
                    String msg = parts.length > 1 ? parts[1] : "Xóa phiên thất bại!";
                    showMessage("❌ " + msg, "red");
                    ToastManager.show(ToastManager.Type.DANGER, "❌ " + msg);
                }
                loadFromServer();
            });
        }).start();
    }

    @FXML
    public void handleCreateAuction() {
        Stage stage = (Stage) auctionTable.getScene().getWindow();
        new CreateAuctionView(stage, username).show();
    }

    @FXML
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> loadFromServer()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) { autoRefreshTimeline.stop(); autoRefreshTimeline = null; }
    }

    public void handleBack() {
        stopAutoRefresh();
        // FIX: dọn dẹp pushListener để tránh memory leak và callback sau khi thoát
        if (pushListener != null) {
            ServerConnection.getInstance().removePushListener(pushListener);
            pushListener = null;
        }
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