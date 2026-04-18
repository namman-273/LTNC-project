package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

public class AuctionRoomController {

    @FXML private Label timerLabel;
    @FXML private Label livePriceLabel;
    @FXML private Label topBidderLabel;
    @FXML private ListView<String> bidHistoryList;

    private ObservableList<String> history = FXCollections.observableArrayList();
    private int secondsRemaining = 600; // 10 phút = 600 giây

    @FXML
    public void initialize() {
        bidHistoryList.setItems(history);
        history.add("Hệ thống: Phiên đấu giá bắt đầu!");

        // BẮT ĐẦU CHẠY ĐỒNG HỒ
        startTimer();
    }

    private void startTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsRemaining--;
            int minutes = secondsRemaining / 60;
            int secs = secondsRemaining % 60;

            // Cập nhật lên giao diện (định dạng 00:00)
            timerLabel.setText(String.format("00:%02d:%02d", minutes, secs));

            if (secondsRemaining <= 0) {
                timerLabel.setText("HẾT GIỜ!");
                history.add(0, "Hệ thống: Phiên đấu giá đã kết thúc!");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void add50k() { updateBid(50000); }
    @FXML
    private void add100k() { updateBid(100000); }
    @FXML
    private void add500k() { updateBid(500000); }

    private void updateBid(int amount) {
        history.add(0, "Bạn vừa đặt thêm " + String.format("%,d", amount) + " VNĐ");
    }
}