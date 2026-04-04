public class Admin extends User{
    public Admin(String id, String username) { super(id, username); }

    @Override
    public void displayInfo() {
        System.out.println("[Admin] ID: " + id + ", Name: " + username);
    }
}
