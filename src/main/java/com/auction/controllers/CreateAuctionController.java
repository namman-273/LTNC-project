package com.auction.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateAuctionController implements Initializable {

    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField durationField;
    @FXML private Label messageLabel;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeComboBox.setItems(FXCollections.observableArrayList("ART", "ELECTRONICS", "VEHICLE"));
        typeComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleCreate() {
        String type = typeComboBox.getValue();
        String name = nameField.getText().trim();
        String price = priceField.getText().trim();
        String duration = durationField.getText().trim();

        if (name.isEmpty() || price.isEmpty() || duration.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            Double.parseDouble(price);
            Long.parseLong(duration);
        } catch (NumberFormatException e) {
            showError("Giá và thời gian phải là số!");
            return;
        }

        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String response = conn.sendAndReceive(
                    "CREATE_AUCTION|" + type + "|" + name + "|" + price + "|" + duration
            );
            System.out.println("Create auction response: " + response);

            javafx.application.Platform.runLater(() -> {
                if (response != null && response.startsWith("SUCCESS")) {
                    showSuccess("Tạo phiên thành công! Đang chuyển về danh sách...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            javafx.application.Platform.runLater(() -> {
                                Stage stage = (Stage) nameField.getScene().getWindow();
                                AuctionListView listView = new AuctionListView(stage, username);
                                listView.show();
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    showError("Tạo phiên thất bại! " + (response != null ? response : ""));
                }
            });
        }).start();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        AuctionListView listView = new AuctionListView(stage, username);
        listView.show();
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}