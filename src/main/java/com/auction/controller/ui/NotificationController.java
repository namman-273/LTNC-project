package com.auction.controller.ui;

import com.auction.util.ui.NotificationManager;
import com.auction.views.java.AuctionListView;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML private ListView<String> notificationList;
    @FXML private Label countLabel;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        notificationList.setItems(NotificationManager.getInstance().getAll());
        notificationList.setCellFactory(lv -> new NotificationCell());
        updateCount();

        NotificationManager.getInstance().getAll().addListener(
                (javafx.collections.ListChangeListener<String>) c -> updateCount()
        );
    }

    private void updateCount() {
        int count = NotificationManager.getInstance().size();
        if (countLabel != null) {
            countLabel.setText(String.valueOf(count));
            countLabel.setVisible(count > 0);
            countLabel.setManaged(count > 0);
        }
    }

    @FXML
    private void handleClear() {
        NotificationManager.getInstance().clear();
        updateCount();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) notificationList.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    // ─── Custom Cell ─────────────────────────────────────────────────────────

    private static class NotificationCell extends ListCell<String> {

        private final HBox root     = new HBox(12);
        private final Label icon    = new Label();
        private final VBox content  = new VBox(4);
        private final Label message = new Label();
        private final Label time    = new Label();

        NotificationCell() {
            icon.setPrefSize(40, 40);
            icon.setMinSize(40, 40);
            icon.setAlignment(Pos.CENTER);
            icon.setStyle(
                    "-fx-background-radius: 12;" +
                            "-fx-font-size: 16px;");

            message.setWrapText(true);
            message.setMaxWidth(340);
            message.setStyle("-fx-font-size: 13px; -fx-text-fill: #1F2937;");

            time.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

            HBox.setHgrow(content, Priority.ALWAYS);
            content.getChildren().addAll(message, time);

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 16, 10, 16));
            root.getChildren().addAll(icon, content);

            setGraphic(root);
            setText(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("");
                return;
            }

            // Tách thời gian và nội dung: "dd/MM HH:mm  •  message"
            String[] split = item.split("  •  ", 2);
            String timeStr = split.length > 1 ? split[0].trim() : "";
            String msgStr  = split.length > 1 ? split[1].trim() : item;

            time.setText(timeStr);
            message.setText(msgStr);

            // Phân loại icon + màu theo nội dung
            if (msgStr.contains("🎉") || msgStr.contains("thắng")) {
                icon.setText("🎉");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #D1FAE5;");
                setStyle("-fx-background-color: #F0FDF4; -fx-border-color: transparent transparent #DCFCE7 transparent; -fx-border-width: 1;");
            } else if (msgStr.contains("⚠️") || msgStr.contains("vượt giá") || msgStr.contains("OUTBID")) {
                icon.setText("⚠️");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #FEF3C7;");
                setStyle("-fx-background-color: #FFFBEB; -fx-border-color: transparent transparent #FDE68A transparent; -fx-border-width: 1;");
            } else if (msgStr.contains("💰") || msgStr.contains("Hoàn") || msgStr.contains("nạp")) {
                icon.setText("💰");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #DBEAFE;");
                setStyle("-fx-background-color: #EFF6FF; -fx-border-color: transparent transparent #BFDBFE transparent; -fx-border-width: 1;");
            } else if (msgStr.contains("⏱") || msgStr.contains("gia hạn")) {
                icon.setText("⏱");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #FFE4E6;");
                setStyle("-fx-background-color: #FFF1F2; -fx-border-color: transparent transparent #FECDD3 transparent; -fx-border-width: 1;");
            } else if (msgStr.contains("🆕") || msgStr.contains("mới")) {
                icon.setText("🆕");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #EDE9FE;");
                setStyle("-fx-background-color: #F5F3FF; -fx-border-color: transparent transparent #DDD6FE transparent; -fx-border-width: 1;");
            } else {
                icon.setText("🔔");
                icon.setStyle(icon.getStyle() + "-fx-background-color: #F3F4F6;");
                setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #F3F4F6 transparent; -fx-border-width: 1;");
            }

            setGraphic(root);
            setText(null);
        }
    }
}
