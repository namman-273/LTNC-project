package com.auction.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SellerAdminTest {

    @Test void sellerRole()         { assertEquals("SELLER", new Seller("s","h").getRole()); }
    @Test void sellerUsername()     { assertEquals("seller1", new Seller("seller1","h").getUsername()); }
    @Test void sellerDisplayInfo()  { assertDoesNotThrow(new Seller("s","h")::displayInfo); }
    @Test void adminRole()          { assertEquals("ADMIN", new Admin("a","h").getRole()); }
    @Test void adminUsername()      { assertEquals("admin1", new Admin("admin1","h").getUsername()); }
    @Test void adminDisplayInfo()   { assertDoesNotThrow(new Admin("a","h")::displayInfo); }
    @Test void updateDoesNotThrow() { assertDoesNotThrow(() -> new Seller("s","h").update("BALANCE_CHANGED|+50000|150000")); }
    @Test void toStringHasUsername(){ assertTrue(new Admin("admin99","h").toString().contains("admin99")); }
}