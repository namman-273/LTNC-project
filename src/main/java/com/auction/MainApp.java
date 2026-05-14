package com.auction;

import com.auction.views.java.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp {

  // Tạo một Static Inner Class để giữ logic JavaFX
  public static class JavaFxMain extends Application {
    @Override
    public void start(Stage primaryStage) {
      LoginView loginView = new LoginView(primaryStage);
      loginView.show();
    }
  }

  // Hàm main chính bây giờ là "thuần Java"
  public static void main(String[] args) {
    // Gọi launch thông qua class trung gian JavaFxMain
    Application.launch(JavaFxMain.class, args);
  }
}