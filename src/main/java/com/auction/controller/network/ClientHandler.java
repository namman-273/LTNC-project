package com.auction.controller.network;

import com.auction.controller.command.AddAutoBidCommand;
import com.auction.controller.command.BidCommand;
import com.auction.controller.command.ChangePasswordCommand;
import com.auction.controller.command.ClientCommand;
import com.auction.controller.command.CreateAuctionCommand;
import com.auction.controller.command.DeleteAuctionCommand;
import com.auction.controller.command.DepositCommand;
import com.auction.controller.command.EndAuctionCommand;
import com.auction.controller.command.GetBalanceCommand;
import com.auction.controller.command.GetBidHistoryCommand;
import com.auction.controller.command.GetHistoryCommand;
import com.auction.controller.command.GetProfileCommand;
import com.auction.controller.command.GetWatchlistCommand;
import com.auction.controller.command.ListAuctionsCommand;
import com.auction.controller.command.LoginCommand;
import com.auction.controller.command.RegisterCommand;
import com.auction.controller.command.UpdateEmailCommand;
import com.auction.controller.command.UnwatchCommand;
import com.auction.controller.command.WatchCommand;
import com.auction.model.entities.user.User;
import com.auction.model.observer.AuctionParticipant;
import com.auction.network.protocol.Protocol;
import com.auction.service.auctionservice.AuctionService;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * moi 1 clienthandler phuc vu 1 socket.
 */
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

    commandMap.put(Protocol.CMD_REGISTER, new RegisterCommand());
    commandMap.put(Protocol.CMD_LOGIN, new LoginCommand());
    commandMap.put(Protocol.CMD_LIST_AUCTIONS, new ListAuctionsCommand());
    commandMap.put(Protocol.CMD_CREATE_AUCTION, new CreateAuctionCommand());
    commandMap.put(Protocol.CMD_END_AUCTION, new EndAuctionCommand());
    commandMap.put(Protocol.CMD_BID, new BidCommand());
    commandMap.put(Protocol.CMD_DEPOSIT, new DepositCommand());
    commandMap.put(Protocol.CMD_GET_BALANCE, new GetBalanceCommand());
    commandMap.put(Protocol.CMD_WATCH, new WatchCommand());
    commandMap.put(Protocol.CMD_UNWATCH, new UnwatchCommand());
    commandMap.put(Protocol.CMD_DELETE_AUCTION, new DeleteAuctionCommand());
    commandMap.put(Protocol.CMD_GET_HISTORY, new GetHistoryCommand());
    commandMap.put(Protocol.CMD_GET_WATCHLIST, new GetWatchlistCommand());
    commandMap.put(Protocol.CMD_ADD_AUTO_BID, new AddAutoBidCommand());
    commandMap.put(Protocol.CMD_GET_BID_HISTORY, new GetBidHistoryCommand());
    commandMap.put(Protocol.CMD_GET_PROFILE, new GetProfileCommand());
    commandMap.put(Protocol.CMD_UPDATE_EMAIL, new UpdateEmailCommand());
    commandMap.put(Protocol.CMD_UPDATE_PASSWORD, new ChangePasswordCommand());
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
        if (parts.length == 0) {
          continue;
        }

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
  // CÔNG CỤ HỖ TRỢ CHO CÁC COMMAND CLASSES
  // ====================================================================================

  /**
   * check du thong tin chua.
   */
  public boolean validatePayload(String[] parts, int expectedLength) {
    if (parts == null || parts.length < expectedLength) {
      sendMessage(
          Protocol.ERROR + Protocol.SEPARATOR
              + "Thiếu thông tin yêu cầu. Cần ít nhất " + expectedLength + " phần.");
      return false;
    }
    return true;
  }

  /**
   * gui tin nhan qua socket de controller fe lay tin.
   */
  public final void sendMessage(final String msg) {
    try {
      if (out != null && !socket.isClosed()) {
        out.println(msg);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public final void update(final String msg) {
    this.sendMessage(msg);
  }

  @Override
  public String getAssociatedUsername() {
    return (this.currentUser != null) ? this.currentUser.getUsername() : null;
  }

  public User getCurrentUser() {
    return this.currentUser;
  }

  /**
   * chinh user moi.
   */
  public void setCurrentUser(User user) {
    // Nếu đang có user cũ, hủy đăng ký trước
    if (this.currentUser != null) {
      ConnectionManager.getInstance().unregisterConnection(this.currentUser.getUsername());
    }

    this.currentUser = user;

    // Đăng ký user mới với ConnectionManager
    if (user != null) {
      ConnectionManager.getInstance().registerConnection(user.getUsername(), this);
    }
  }

  private void cleanUp() {
    try {
      // Hủy đăng ký với ConnectionManager
      if (currentUser != null) {
        ConnectionManager.getInstance().unregisterConnection(currentUser.getUsername());
      }

      AuctionService.getInstance().removeObserverFromAll(this);
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      System.err.println("Lỗi đóng tài nguyên: " + e.getMessage());
    }
  }
}