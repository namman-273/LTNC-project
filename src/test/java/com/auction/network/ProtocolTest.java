package com.auction.network;
 import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import org.junit.jupiter.api.Test;
 
public class ProtocolTest {
 
    // --- Separator ---
 
    @Test
    void separatorIsNotNull() {
        assertNotNull(Protocol.SEPARATOR);
    }
 
    @Test
    void separatorContainsPipeCharacter() {
        assertTrue(Protocol.SEPARATOR.contains("|"),
            "SEPARATOR must contain pipe character for message splitting");
    }
 
    @Test
    void separatorIsNotEmpty() {
        assertFalse(Protocol.SEPARATOR.isEmpty());
    }
 
    // --- Commands: not null, not empty, uppercase ---
 
    @Test
    void cmdRegisterIsNotNull() {
        assertNotNull(Protocol.CMD_REGISTER);
    }
 
    @Test
    void cmdLoginIsNotNull() {
        assertNotNull(Protocol.CMD_LOGIN);
    }
 
    @Test
    void cmdListAuctionsIsNotNull() {
        assertNotNull(Protocol.CMD_LIST_AUCTIONS);
    }
 
    @Test
    void cmdBidIsNotNull() {
        assertNotNull(Protocol.CMD_BID);
    }
 
    @Test
    void cmdCreateAuctionIsNotNull() {
        assertNotNull(Protocol.CMD_CREATE_AUCTION);
    }
 
    @Test
    void cmdEndAuctionIsNotNull() {
        assertNotNull(Protocol.CMD_END_AUCTION);
    }
 
    @Test
    void cmdGetHistoryIsNotNull() {
        assertNotNull(Protocol.CMD_GET_HISTORY);
    }
 
    // --- Responses: not null ---
 
    @Test
    void resRegisterSuccessIsNotNull() {
        assertNotNull(Protocol.RES_REGISTER_SUCCESS);
    }
 
    @Test
    void resRegisterFailedIsNotNull() {
        assertNotNull(Protocol.RES_REGISTER_FAILED);
    }
 
    @Test
    void resLoginSuccessIsNotNull() {
        assertNotNull(Protocol.RES_LOGIN_SUCCESS);
    }
 
    @Test
    void resLoginFailedIsNotNull() {
        assertNotNull(Protocol.RES_LOGIN_FAILED);
    }
 
    @Test
    void resBidSuccessIsNotNull() {
        assertNotNull(Protocol.RES_BID_SUCCESS);
    }
 
    @Test
    void resEndSuccessIsNotNull() {
        assertNotNull(Protocol.RES_END_SUCCESS);
    }
 
    @Test
    void resHistoryIsNotNull() {
        assertNotNull(Protocol.RES_HISTORY);
    }
 
    @Test
    void resSuccessIsNotNull() {
        assertNotNull(Protocol.RES_SUCCESS);
    }
 
    @Test
    void errorConstantIsNotNull() {
        assertNotNull(Protocol.ERROR);
    }
 
    // --- Commands must be distinct from each other ---
 
    @Test
    void cmdRegisterDistinctFromCmdLogin() {
        assertNotEquals(Protocol.CMD_REGISTER, Protocol.CMD_LOGIN);
    }
 
    @Test
    void cmdBidDistinctFromCmdCreateAuction() {
        assertNotEquals(Protocol.CMD_BID, Protocol.CMD_CREATE_AUCTION);
    }
 
    @Test
    void cmdEndAuctionDistinctFromCmdListAuctions() {
        assertNotEquals(Protocol.CMD_END_AUCTION, Protocol.CMD_LIST_AUCTIONS);
    }
 
    // --- Responses must be distinct from each other ---
 
    @Test
    void resLoginSuccessDistinctFromResLoginFailed() {
        assertNotEquals(Protocol.RES_LOGIN_SUCCESS, Protocol.RES_LOGIN_FAILED);
    }
 
    @Test
    void resRegisterSuccessDistinctFromResRegisterFailed() {
        assertNotEquals(Protocol.RES_REGISTER_SUCCESS, Protocol.RES_REGISTER_FAILED);
    }
 
    @Test
    void errorDistinctFromResSuccess() {
        assertNotEquals(Protocol.ERROR, Protocol.RES_SUCCESS);
    }
 
    // --- Commands distinct from responses (no accidental overlap) ---
 
    @Test
    void cmdRegisterDistinctFromResRegisterSuccess() {
        assertNotEquals(Protocol.CMD_REGISTER, Protocol.RES_REGISTER_SUCCESS);
    }
 
    @Test
    void cmdLoginDistinctFromResLoginSuccess() {
        assertNotEquals(Protocol.CMD_LOGIN, Protocol.RES_LOGIN_SUCCESS);
    }
 
    @Test
    void cmdBidDistinctFromResBidSuccess() {
        assertNotEquals(Protocol.CMD_BID, Protocol.RES_BID_SUCCESS);
    }
 
    // --- Message format construction using SEPARATOR ---
 
    @Test
    void messageBuildWithSeparatorCanBeSplit() {
        String msg = Protocol.RES_LOGIN_SUCCESS + Protocol.SEPARATOR + "BIDDER" + Protocol.SEPARATOR + "alice";
        String[] parts = msg.split("\\s*\\|\\s*");
        assertTrue(parts.length >= 3,
            "Message built with SEPARATOR must be splittable into at least 3 parts");
    }
 
    @Test
    void errorMessageBuildContainsErrorPrefix() {
        String msg = Protocol.ERROR + Protocol.SEPARATOR + "Some error detail";
        assertTrue(msg.startsWith(Protocol.ERROR),
            "Error message must start with ERROR constant");
    }
 
    @Test
    void bidSuccessMessageContainsSeparator() {
        String msg = Protocol.RES_BID_SUCCESS + Protocol.SEPARATOR + "auction-01" + Protocol.SEPARATOR + "1500.0";
        assertTrue(msg.contains("|"),
            "BID_SUCCESS message must contain pipe separator");
    }
 
    // --- Commands are uppercase (consistency guard) ---
 
    @Test
    void cmdRegisterIsUpperCase() {
        assertEquals(Protocol.CMD_REGISTER, Protocol.CMD_REGISTER.toUpperCase());
    }
 
    @Test
    void cmdLoginIsUpperCase() {
        assertEquals(Protocol.CMD_LOGIN, Protocol.CMD_LOGIN.toUpperCase());
    }
 
    @Test
    void cmdBidIsUpperCase() {
        assertEquals(Protocol.CMD_BID, Protocol.CMD_BID.toUpperCase());
    }
 
    @Test
    void errorConstantIsUpperCase() {
        assertEquals(Protocol.ERROR, Protocol.ERROR.toUpperCase());
    }
}