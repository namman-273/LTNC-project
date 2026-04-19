import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;

public class Main {
    public static void main(String[] args) {
        // 1. Tạo com.auction.model.Item với giá khởi điểm là 500.0
        // (Giả sử bạn có lớp con com.auction.model.Electronics kế thừa từ com.auction.model.Item)
        Item laptop = new Electronics("IT01", "Laptop Dell", 500.0, 12);

        // 2. Tạo com.auction.model.Auction chỉ với ID và com.auction.model.Item (Đúng như code của bạn)
        Auction auction = new Auction("AUC-001", laptop);

        Bidder tung = new Bidder("U01", "Tùng");

        System.out.println("=== KIỂM TRA HỆ THỐNG ĐẤU GIÁ ===");
        laptop.displayInfo();



        auction.startAuction(); // Giả sử hàm này đổi status sang RUNNING


        // --- TEST CASE 3: Đặt giá hợp lệ ---
        try {
            System.out.print("Test 3 (Đặt giá 700.0): ");
            auction.processNewBid(tung, 800.0);
            System.out.println("THÀNH CÔNG! Giá mới: " + auction.getCurrentPrice());
        } catch (Exception e) {
            System.err.println("LỖI: " + e.getMessage());
        }

        // --- TEST CASE 4: Kiểm tra Authentication (null) ---
        try {
            System.out.print("Test 4 (Người dùng null): ");
            auction.processNewBid(null, 1000.0);
        } catch (Exception e) {
            System.err.println("THẤT BẠI: " + e.getMessage());
        }
    }
}