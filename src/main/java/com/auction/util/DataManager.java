package com.auction.util;

import com.auction.service.AuctionService;
import java.io.*;

public class DataManager {
    // Tên file sẽ được tạo ra trong thư mục dự án
    private static final String DATA_FILE = "auction_data.dat";

    /**
     * LƯU DỮ LIỆU: Bê nguyên cái AuctionService (đã có Map auctions) xuống file
     */
    public static void saveLibrary() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            // Vì AuctionService là Singleton, ta lấy instance hiện tại để lưu
            oos.writeObject(AuctionService.getInstance());
            System.out.println(">>> [DataManager] Đã lưu dữ liệu thành công!");
        } catch (IOException e) {
            System.err.println(">>> [DataManager] Lỗi lưu file: " + e.getMessage());
        }
    }

    /**
     * TẢI DỮ LIỆU: Đọc file cũ và dựng lại AuctionService
     */
    public static void loadLibrary() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            System.out.println(">>> [DataManager] Không tìm thấy dữ liệu cũ. Khởi tạo mới.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            // Đọc đối tượng từ file
            AuctionService loadedService = (AuctionService) ois.readObject();

            // Cập nhật lại "Ngai vàng" Singleton bằng bản vừa load
            AuctionService.setInstance(loadedService);

            System.out.println(">>> [DataManager] Khôi phục dữ liệu thành công.");
        } catch (Exception e) {
            System.err.println(">>> [DataManager] Lỗi load file: " + e.getMessage());
        }
    }
}