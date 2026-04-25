package com.auction.util;

import com.auction.service.AuctionService;
import com.auction.model.Auction;
import java.io.*;
import java.util.Map;

public final class DataManager { // Thêm final để đúng Checkstyle
    private static final String DATA_FILE = "auction_data.dat";
    private static DataManager instance;

    // Singleton Pattern để giải quyết lỗi
    private DataManager() {}

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    /**
     * LƯU DỮ LIỆU: Chỉ lưu Map chứa các phiên đấu giá
     */
    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            // Chỉ lưu data core (Map auctions), không lưu cả Service
            oos.writeObject(AuctionService.getInstance().getAuctionsMap());
            System.out.println(">>> [DataManager] Dữ liệu đã được lưu!");
        } catch (IOException e) {
            System.err.println(">>> [DataManager] Lỗi lưu file: " + e.getMessage());
        }
    }

    /**
     * TẢI DỮ LIỆU: Đọc Map và đẩy ngược vào Service hiện tại
     */
    @SuppressWarnings("unchecked")
    public void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            // Đọc Map data
            Map<String, Auction> loadedAuctions = (Map<String, Auction>) ois.readObject();
            
            // Đổ data vào Service đang chạy thay vì thay thế cả Service
            AuctionService.getInstance().setAuctions(loadedAuctions);
            
            System.out.println(">>> [DataManager] Khôi phục " + loadedAuctions.size() + " phiên đấu giá.");
        } catch (Exception e) {
            System.err.println(">>> [DataManager] Lỗi load file: " + e.getMessage());
        }
    }
}