package com.auction.controller.network;

import com.auction.controller.command.*;
import com.auction.model.entities.user.User;
import com.auction.model.observer.AuctionParticipant;
import com.auction.network.protocol.Protocol;
import com.auction.service.AuctionService;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable, AuctionParticipant {

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  
  // Mở public để các Command có thể truy cập
  public final Gson gson = new com.google.gson.GsonBuilder()
      .registerTypeAdapter(java.time.LocalDateTime.class,
          (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc,
              context) -> new com.google.gson.JsonPrimitive(src.toString()))
      .create();
      
  private User currentUser;
  private final Map<String, ClientCommand> commandMap = new HashMap<>();

  public ClientHandler(final Socket socket) {
    this.socket = socket;
    initCommands();
  }

  private void initCommands() {
    // Nạp đạn cho 14 lệnh
    commandMap.put(Protocol.CMD_REGISTER, new RegisterCommand());
    commandMap.put(Protocol.CMD_LOGIN, new LoginCommand());
    commandMap.put(Protocol.CMD_LIST_AUCTIONS, new ListAuctionsCommand());
    commandMap.put(Protocol.CMD_CREATE_AUCTION, new CreateAuctionCommand());
    commandMap.put(Protocol.CMD_END_AUCTION, new EndAuctionCommand());
    commandMap.put(Protocol.CMD_DELETE_AUCTION, new DeleteAuctionCommand());
    commandMap.put(Protocol.CMD_BID, new BidCommand());
    commandMap.put(Protocol.CMD_DEPOSIT, new DepositCommand());
    commandMap.put(Protocol.CMD_GET_BALANCE, new GetBalanceCommand());
    commandMap.put(Protocol.CMD_GET_HISTORY, new GetHistoryCommand());
    commandMap.put(Protocol.CMD_WATCH, new WatchCommand());
    commandMap.put(Protocol.CMD_UNWATCH, new UnwatchCommand());
    commandMap.put(Protocol.CMD_GET_WATCHLIST, new GetWatchlistCommand());
    commandMap.put(Protocol.CMD_ADD_AUTO_BID, new AddAutoBidCommand());
  }

  @Override
  public final void run() {
    AuctionService auctionService = AuctionService.getInstance();
    try {
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      String request;
      while ((request = in.readLine()) != null) {
        String[] parts = request.trim().split("\\|");
        if (parts.length == 0) continue;
        
        ClientCommand command = commandMap.get(parts[0]);
        if (command != null) {
          command.execute(parts, this, auctionService);
        } else {
          sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Lệnh không hợp lệ");
        }
      }
    } catch (IOException e) {
      System.out.println("Một client đã ngắt kết nối.");
    } finally {
      cleanUp();
    }
  }

  // ====================================================================================
  //  gọi ngược lại Command
  // ====================================================================================
  
  public void handleRegister(final String[] parts) {
    commandMap.get(Protocol.CMD_REGISTER).execute(parts, this, AuctionService.getInstance());
  }

  public void handleLogin(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_LOGIN).execute(parts, this, auctionService);
  }

  public void handleListAuctions(AuctionService auctionService) {
    commandMap.get(Protocol.CMD_LIST_AUCTIONS).execute(new String[]{Protocol.CMD_LIST_AUCTIONS}, this, auctionService);
  }

  public void handleCreateAuction(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_CREATE_AUCTION).execute(parts, this, auctionService);
  }

  public void handleEndAuction(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_END_AUCTION).execute(parts, this, auctionService);
  }

  public void handleDeleteAuction(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_DELETE_AUCTION).execute(parts, this, auctionService);
  }

  public void handleBid(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_BID).execute(parts, this, auctionService);
  }

  public void handleDeposit(String[] parts) {
    commandMap.get(Protocol.CMD_DEPOSIT).execute(parts, this, AuctionService.getInstance());
  }

  public void handleGetBalance() {
    commandMap.get(Protocol.CMD_GET_BALANCE).execute(new String[]{Protocol.CMD_GET_BALANCE}, this, AuctionService.getInstance());
  }

  public void handleGetHistory(String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_GET_HISTORY).execute(parts, this, auctionService);
  }

  public void handleWatch(String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_WATCH).execute(parts, this, auctionService);
  }

  public void handleUnwatch(String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_UNWATCH).execute(parts, this, auctionService);
  }

  public void handleGetWatchlist(AuctionService auctionService) {
    commandMap.get(Protocol.CMD_GET_WATCHLIST).execute(new String[]{Protocol.CMD_GET_WATCHLIST}, this, auctionService);
  }

  public void handleAddAutoBid(final String[] parts, AuctionService auctionService) {
    commandMap.get(Protocol.CMD_ADD_AUTO_BID).execute(parts, this, auctionService);
  }

  // ====================================================================================
  // CÔNG CỤ HỖ TRỢ CHO CÁC COMMAND CLASSES
  // ====================================================================================

  public boolean validatePayload(String[] parts, int expectedLength) {
    if (parts == null || parts.length < expectedLength) {
      sendMessage("ERROR|Thiếu tham số yêu cầu. Cần ít nhất " + expectedLength + " phần.");
      return false;
    }
    return true;
  }

  public final void sendMessage(final String msg) {
    try {
      if (out != null && !socket.isClosed()) out.println(msg);
    } catch (Exception e) {}
  }

  public final void update(final String msg) {
    this.sendMessage(msg);
  }

  @Override
  public String getAssociatedUsername() {
    return (this.currentUser != null) ? this.currentUser.getUsername() : null;
  }

  public User getCurrentUser() { return this.currentUser; }
  public void setCurrentUser(User user) { this.currentUser = user; }

  private void cleanUp() {
    try {
      AuctionService.getInstance().removeObserverFromAll(this);
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException e) {
      System.err.println("Lỗi đóng tài nguyên: " + e.getMessage());
    }
  }
}