package com.auction.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.auction.controllers.BidController;

public class BidView {

    private Stage stage;
    private String auctionId;
    private String itemName;
    private String currentPrice;
    private String status;
    private String username;

    // FIX [RegexpSingleline]: xoá khoảng trắng thừa cuối dòng
    public BidView(Stage stage, String auctionId, String itemName,
                   String currentPrice, String status, String username) {
        this.stage = stage;
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.username = username;
    }

    public void show() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    // FIX [Indentation]: tăng indent từ 16 lên 20 spaces
                    getClass().getResource("/com/auction/views/BidView.fxml")
            );
            Parent root = loader.load();

            BidController controller = loader.getController();
            String password = com.auction.util.SessionManager.getInstance().getPassword();
            controller.setData(auctionId, itemName, currentPrice, status, username, password);

            Scene scene = new Scene(root);
            stage.setTitle("Đấu giá - " + itemName);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi load FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
