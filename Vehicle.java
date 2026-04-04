public class Vehicle extends Item{
    private int model;
    public Vehicle(String id, String name, double price, int model) {
        super(id, name, price);
        this.model=model;
    }

    @Override
    public void displayInfo() {
        System.out.println("[Vehicle] "+itemName+" - Mẫu:"+ model);
        System.out.println("Giá khởi điểm:"+startingPrice);
    }
}
