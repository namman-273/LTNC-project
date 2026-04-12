import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load cái màn hình bạn vừa vẽ
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AuctionListView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Chương trình Đấu Giá - Nhóm 13");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}