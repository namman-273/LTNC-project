package com.auction.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.network.Protocol;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateAuctionController implements Initializable {

    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField durationField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private Label messageLabel;

    private String username;

    public void setUsername(String username) { this.username = username; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeComboBox.setItems(FXCollections.observableArrayList("ART", "ELECTRONICS", "VEHICLE"));
        typeComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleCreate() {
        String type        = typeComboBox.getValue();
        String name        = nameField.getText().trim();
        String price       = priceField.getText().trim();
        String duration    = durationField.getText().trim();
        String imageUrl    = imageUrlField != null ? imageUrlField.getText().trim() : "";
        String description = descriptionArea != null ? descriptionArea.getText().trim() : "";

        // Không tự validate — gửi thẳng lên BE
        // BE expect: CREATE_AUCTION|type|name|price|duration|imageUrl|description
        new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            if (!conn.isConnected()) {
                Platform.runLater(() -> showError("Mất kết nối server!"));
                return;
            }

            String response = conn.sendAndReceive(
                    Protocol.CMD_CREATE_AUCTION + Protocol.SEPARATOR
                            + type        + Protocol.SEPARATOR
                            + name        + Protocol.SEPARATOR
                            + price       + Protocol.SEPARATOR
                            + duration    + Protocol.SEPARATOR
                            + imageUrl    + Protocol.SEPARATOR
                            + description
            );
            System.out.println("Create auction response: " + response);

            Platform.runLater(() -> {
                if (response == null) { showError("Mất kết nối server!"); return; }

                String[] parts = response.split("\\" + Protocol.SEPARATOR);

                if (response.startsWith(Protocol.RES_SUCCESS)) {
                    String msg = parts.length > 1 ? parts[1] : "Tạo phiên thành công!";
                    showSuccess(msg + " Đang chuyển về danh sách...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(() -> {
                                Stage stage = (Stage) nameField.getScene().getWindow();
                                new AuctionListView(stage, username).show();
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                } else {
                    String errorMsg = parts.length > 1 ? parts[1] : "Tạo phiên thất bại!";
                    showError(errorMsg);
                }
            });
        }).start();
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        new AuctionListView(stage, username).show();
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