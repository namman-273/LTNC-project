
import java.util.concurrent.locks.Lock;

import com.auction.model.Art;
import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.network.AuctionServer;
import com.auction.service.AuctionService;
import com.auction.util.DataManager;

public class Main {
 public static void main(String[] args) {
    DataManager.loadLibrary();
    
    // 1. Tạo Service
    AuctionService service = AuctionService.getInstance();

    // 2. Thêm vài món đồ "High-end" để test
    // Giả sử bạn có class Art kế thừa Item
    Item tranhSonDau = new Art("AUC01", "Tranh Hoa Sen", 1000.0);
    Auction phien1 = new Auction("AUC01", tranhSonDau);
    
    service.addAuction(phien1);
    phien1.startAuction(); // Đừng quên mở phiên thì mới BID được!
    
    //Hồi sinh các biến transient (Lock, Observers) ,vì sau khi load từ file, các biến này đang bị null
    for (Auction auction : service.getAllAuctions()) {
        auction.restoreTransients();
    }

    // 3. Khởi động Server
    AuctionServer server = new AuctionServer(8080, service);
    server.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n[Hệ thống] Đang tự động sao lưu dữ liệu trước khi đóng...");
      DataManager.saveLibrary();
      System.out.println("[Hệ thống] Tạm biệt!");
    }));
    }
}