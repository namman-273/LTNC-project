public class AuctionService {

    public AuctionSession createSession(String itemName, double startPrice) {
        return new AuctionSession(itemName, startPrice);
    }

    public String placeBid(AuctionSession session, double bidPrice) {

        if (!session.isOpen()) {
            return "Lỗi: Phiên đã đóng!";
        }

        if (bidPrice <= session.getCurrentPrice()) {
            return "Lỗi: Giá phải lớn hơn giá hiện tại!";
        }

        session.updatePrice(bidPrice);
        return "Đặt giá thành công! Giá mới: " + bidPrice;
    }
}