public class Bidder extends User{
    public Bidder(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[Bidder] ID: " + id + ", Name: " + username);
    }
    public String getName(){return username;}
}
