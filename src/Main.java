import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Gọi file thiết kế giao diện từ thư mục views
        Parent root = FXMLLoader.load(getClass().getResource("/views/LoginView.fxml"));

        // Cài đặt tiêu đề và kích thước cho cửa sổ
        primaryStage.setTitle("Hệ thống Đấu giá 1388");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Lệnh này sẽ kích hoạt giao diện lên
    }
}