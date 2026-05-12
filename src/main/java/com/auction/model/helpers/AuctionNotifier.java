package com.auction.model.helpers;

import com.auction.model.entities.Auction;
import com.auction.model.entities.user.User;
import com.auction.model.observer.AuctionParticipant;
import com.auction.model.observer.Observer;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AuctionNotifier {

  public void notifyAllParticipants(Auction auction, List<Observer> observers, ExecutorService notifyExecutor,
      String message, User excludeUser) {
    if (observers == null || observers.isEmpty())
      return;

    for (Observer observer : observers) {

      if (observer instanceof AuctionParticipant) {
        AuctionParticipant participant = (AuctionParticipant) observer;
        // Bỏ qua không gửi cho người vừa tạo ra hành động này (để tránh tự spam chính
        // mình)
        if (excludeUser != null
            && excludeUser.getUsername().equals(participant.getAssociatedUsername())) {
          continue; // Bỏ qua người gửi
        }

      }
      notifyExecutor.submit(() -> {
        try {
          if (observer != null)
            observer.update(message);
        } catch (Exception e) {
          auction.removeObserver(observer);
          // Nếu Socket sập, gỡ luôn khỏi danh sách
          System.out.println("Removed faulty observer: " + e.getMessage());
        }
      });
    }
    System.out.println("[NOTIFICATION] Đã phát sóng thông báo tới " + observers.size() + " đường truyền mạng.");
  }

  public void notifySpecificUser(Auction auction, List<Observer> observers, ExecutorService notifyExecutor,
      String targetUsername, String message) {
    if (observers == null || observers.isEmpty() || targetUsername == null)
      return;

    for (Observer observer : observers) {
      if (observer instanceof AuctionParticipant) {
        AuctionParticipant participant = (AuctionParticipant) observer;
        if (targetUsername.equals(participant.getAssociatedUsername())) {
          notifyExecutor.submit(() -> {
            try {
              observer.update(message);
            } catch (Exception e) {
              auction.removeObserver(observer);
            }
          });
          break; // Tìm thấy và gửi rồi thì dừng vòng lặp
        }
      }
    }
  }
}