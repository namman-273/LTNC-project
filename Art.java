public class Art extends Item{
    private String artist;

    public Art(String id, String name, double price, String artist) {
        super(id, name, price);
        this.artist = artist;
    }

    @Override
    public void displayInfo() {
        System.out.println("[Art] " + itemName + " - Tác giả: " + artist);
        System.out.println("Giá khởi điểm:"+startingPrice);
    }
}
