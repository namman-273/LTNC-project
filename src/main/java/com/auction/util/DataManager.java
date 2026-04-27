package com.auction.util;

import com.auction.service.AuctionService;
import com.auction.model.Auction;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

public final class DataManager { // Thêm final để đúng Checkstyle
    private static final String DATA_FILE = "auction_data.dat";
    private static final String TEMP_FILE = "auction-data.tmp";
    private static DataManager instance;

    // Singleton Pattern để giải quyết lỗi
    private DataManager() {
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    /**
     * LƯU DỮ LIỆU: Atomic Write tránh mất dữ liệu khi crash
     */
    public synchronized void saveData() {
        try {
            //  Ghi dữ liệu vào file TẠM THỜI
            File tempFile = new File(TEMP_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                // Chỉ lưu Map core của Auctions
                oos.writeObject(AuctionService.getInstance().getAuctionsMap());
                oos.flush();
            }

            //  Atomic Rename: Di chuyển file tạm đè lên file chính
            Path source = Paths.get(TEMP_FILE);
            Path target = Paths.get(DATA_FILE);

            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            System.out.println(">>> [DataManager] Dữ liệu đã được lưu an toàn (Atomic Write)!");

        } catch (IOException e) {
            System.err.println(">>> [DataManager] Lỗi lưu file: " + e.getMessage());
            // Xóa file tạm nếu việc ghi bị lỗi giữa chừng
            new File(TEMP_FILE).delete();
        }
    }

    /**
     * TẢI DỮ LIỆU: Đọc Map và đẩy ngược vào Service hiện tại
     */
    @SuppressWarnings("unchecked")
    public void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            System.out.println(">>> [DataManager] Không tìm thấy file dữ liệu, khởi tạo mới.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // Đọc Map data
            Map<String, Auction> loadedAuctions = (Map<String, Auction>) ois.readObject();
            
            // Đổ data vào Service đang chạy
            AuctionService.getInstance().setAuctions(loadedAuctions);
            
            System.out.println(">>> [DataManager] Khôi phục thành công " + loadedAuctions.size() + " phiên đấu giá.");
        } catch (Exception e) {
            System.err.println(">>> [DataManager] Lỗi load file (Dữ liệu có thể bị hỏng): " + e.getMessage());
        }
    }
}