package com.auction.controllers;

import com.auction.util.NotificationManager;
import com.auction.views.AuctionListView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
        updateCount();

        // Cập nhật count khi list thay đổi
        NotificationManager.getInstance().getAll().addListener(
                (javafx.collections.ListChangeListener<String>) c -> updateCount()
        );
    }

    private void updateCount() {
        int count = NotificationManager.getInstance().size();
        if (countLabel != null) {
            countLabel.setText(count > 0
                    ? count + " thông báo"
                    : "Không có thông báo mới");
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
}