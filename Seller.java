public class Seller extends User{
    public Seller(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[Seller] ID: " + id + ", Name: " + username);
    }
}
