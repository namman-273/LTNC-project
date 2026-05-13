package com.auction.service.auctionservice;

import com.auction.model.entities.Auction;
import com.auction.model.entities.user.Bidder;
import com.auction.model.entities.user.User;
import com.auction.service.usermanger.UserManager;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service quản lý watchlist của bidders.
 * Tuân thủ Single Responsibility Principle.
 */
public class WatchlistService {

  private final AuctionRepository auctionRepository;

  public WatchlistService(AuctionRepository auctionRepository) {
    this.auctionRepository = auctionRepository;
  }

  /**
   * Lấy watchlist của user.
   */
  public List<Auction> getWatchlistForUser(String username) {
    User user = UserManager.getInstance().findUserByUsername(username);

    // Kiểm tra xem user có phải là Bidder không
    if (!(user instanceof Bidder)) {
      return Collections.emptyList();
    }

    Bidder bidder = (Bidder) user;
    return bidder.getWatchlist().stream()
        .map(auctionRepository::findById)
        .filter(Objects::nonNull) // Loại bỏ nếu auction không tồn tại
        .collect(Collectors.toList());
  }

  /**
   * Thêm auction vào watchlist của bidder.
   */
  public boolean addToWatchlist(String username, String auctionId) {
    User user = UserManager.getInstance().findUserByUsername(username);

    if (!(user instanceof Bidder)) {
      return false;
    }

    Bidder bidder = (Bidder) user;
    Auction auction = auctionRepository.findById(auctionId);

    if (auction == null) {
      return false;
    }

    return bidder.getWatchlist().add(auctionId);
  }

  /**
   * Xóa auction khỏi watchlist của bidder.
   */
  public boolean removeFromWatchlist(String username, String auctionId) {
    User user = UserManager.getInstance().findUserByUsername(username);

    if (!(user instanceof Bidder)) {
      return false;
    }

    Bidder bidder = (Bidder) user;
    return bidder.getWatchlist().remove(auctionId);
  }
}