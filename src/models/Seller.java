package models;

public class Seller extends User {
    public Seller(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[models.Seller] ID: " + id + ", Name: " + username);
    }
}
