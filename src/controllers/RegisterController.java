package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    private void handleRegister(ActionEvent event) {
        // In ra để kiểm tra xem code đã nhận dữ liệu chưa
        String user = regUsernameField.getText();
        System.out.println("Dương ơi, đang xử lý đăng ký cho: " + user);

        // Chỗ này sau này Nam sẽ code để lưu vào database
    }

    @FXML
    private void backToLogin(ActionEvent event) {
        System.out.println("Quay lại màn hình Login...");
        // Logic chuyển màn hình sẽ viết ở đây
    }
}