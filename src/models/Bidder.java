package models;

public class Bidder extends User {
    public Bidder(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[models.Bidder] ID: " + id + ", Name: " + username);
    }
}
