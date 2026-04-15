package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.*;// Nhớ import file của Nam vào

public class ItemListController {

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, Double> priceColumn;

    @FXML
    public void initialize() {
        // 1. "Nối ống" dữ liệu: Cột tên sẽ lấy từ hàm getName(), cột giá lấy từ getStartingPrice()
        // Sửa "name" thành "itemName" cho đúng tên Nam đặt
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        // 2. Tạo dữ liệu mẫu cho Dương xem thử
        // Vì Item là abstract, chúng mình sẽ dùng lớp con (ví dụ: Electronics hoặc Art)
// để khởi tạo, nhưng vẫn lưu trong danh sách kiểu Item.
        ObservableList<Item> data = FXCollections.observableArrayList(
// Sửa lại thành thế này cho đủ 4 tham số
                new Electronics("001", "Siêu xe Ferrari", 500000.0, 0),
                new Electronics("002", "Đồng hồ vàng 18K", 15000.0, 12)
        );

        // 3. Đổ dữ liệu vào bảng
        itemTable.setItems(data);
    }
}