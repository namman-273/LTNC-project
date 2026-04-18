package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;

public class ItemDetailController {

    @FXML private Label itemNameLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Text descriptionText;
    @FXML private TextField bidAmountField;
    @FXML private ImageView itemImageView;

    // HÀM QUAN TRỌNG: Dùng để thay đổi thông tin tùy theo sản phẩm
    public void setItemData(String name, String startPrice, String currentPrice, String desc, String imageName) {
        itemNameLabel.setText(name);
        startPriceLabel.setText(startPrice + " VNĐ");
        currentPriceLabel.setText(currentPrice + " VNĐ");
        descriptionText.setText(desc);

        // Đoạn này để thay đổi ảnh động
        try {
            Image image = new Image(getClass().getResourceAsStream("/views/" + imageName));
            itemImageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh: " + imageName);
        }
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String bidAmount = bidAmountField.getText();
        System.out.println("Dương đã đặt giá: " + bidAmount);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        System.out.println("Quay lại danh sách...");
    }
}