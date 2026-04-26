package com.auction.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.auction.util.ServerConnection;
import com.auction.views.AuctionListView;
import com.auction.views.RegisterView;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.connect()) {
            errorLabel.setText("Không thể kết nối server!");
            return;
        }

        
        String response = conn.sendAndReceive("LOGIN|" + username + "|" + password);
        System.out.println("Server trả về: " + response);

        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            AuctionListView listView = new AuctionListView(stage, username);
            listView.show();
        } else {
            errorLabel.setText("Sai tên đăng nhập hoặc mật khẩu!");
        }
    }
    @FXML
    private void handleRegister() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        RegisterView registerView = new RegisterView(stage);
        registerView.show();
    }
}