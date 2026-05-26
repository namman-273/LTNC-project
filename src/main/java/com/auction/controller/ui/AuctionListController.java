package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.SessionManager;
import com.auction.util.ui.AlertUtil;
import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.ToastManager;
import com.auction.views.java.AdminDashboardView;
import com.auction.views.java.BalanceView;
import com.auction.views.java.BidHistoryView;
import com.auction.views.java.BidView;
import com.auction.views.java.CreateAuctionView;
import com.auction.views.java.LoginView;
import com.auction.views.java.NotificationView;
import com.auction.views.java.ProfileView;
import com.auction.views.java.SellerView;
import com.auction.views.java.WatchlistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean; // FIX Bug2b: throttle loadFromServer
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AuctionListController implements Initializable {

  @FXML private javafx.scene.layout.StackPane rootBox;
  @FXML private Label   welcomeLabel;
  @FXML private Label   balanceLabel;
  @FXML private FlowPane auctionGrid;
  @FXML private Label     notifBadge;
  @FXML private Button  adminButton;
  @FXML private Button  sellerButton;
  @FXML private Button  watchlistButton;
  @FXML private Button  watchlistIconButton;
  @FXML private Button  sellerIconButton;
  @FXML private Button  adminIconButton;
  @FXML private Label   statusBarLabel;
  @FXML private TextField searchField;

  @FXML private Button btnFilterAll;
  @FXML private Button btnFilterArt;
  @FXML private Button btnFilterElec;
  @FXML private Button btnFilterVehicle;
  @FXML private Button btnFilterOther;
  @FXML private Button btnPriceAll;
  @FXML private Button btnPriceUnder5;
  @FXML private Button btnPriceMid;
  @FXML private Button btnPriceOver50;

  private String activeTypeFilter  = "ALL";
  private String activePriceFilter = "ALL";

  private String username;
  private AuctionRow selectedRow = null;
  private final List<AuctionRow> currentRows = new ArrayList<>();
  private Consumer<String> pushListener;
  private Timeline autoRefreshTimeline;
  private final java.util.Set<String> biddedAuctions = new java.util.HashSet<>();

  // FIX Bug2b: guard để không chạy 2 loadFromServer thread cùng lúc
  private final AtomicBoolean listLoading = new AtomicBoolean(false);

  private final Gson gson = new GsonBuilder()
          .registerTypeAdapter(java.time.LocalDateTime.class,
                  (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                          java.time.LocalDateTime.parse(json.getAsString()))
          .create();

  public void setUsername(String username) {
    this.username = username;
    if (welcomeLabel != null) { 
      welcomeLabel.setText("Xin chào, " + username + "!"); 
    }
    String role = SessionManager.getInstance().getRole();
    if (adminButton != null) {
      adminButton.setVisible("ADMIN".equalsIgnoreCase(role));
      adminButton.setManaged("ADMIN".equalsIgnoreCase(role));
    }
    if (sellerButton != null) {
      sellerButton.setVisible("SELLER".equalsIgnoreCase(role));
      sellerButton.setManaged("SELLER".equalsIgnoreCase(role));
    }
    if (sellerIconButton != null) {
      sellerIconButton.setVisible("SELLER".equalsIgnoreCase(role));
      sellerIconButton.setManaged("SELLER".equalsIgnoreCase(role));
    }
    if (adminIconButton != null) {
      adminIconButton.setVisible("ADMIN".equalsIgnoreCase(role));
      adminIconButton.setManaged("ADMIN".equalsIgnoreCase(role));
    }
    boolean isBidder = !"SELLER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role);
    if (watchlistButton != null) {
      watchlistButton.setVisible(isBidder); 
      watchlistButton.setManaged(isBidder);
    }
    if (watchlistIconButton != null) {
      watchlistIconButton.setVisible(isBidder); 
      watchlistIconButton.setManaged(isBidder);
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    loadFromServer();
    registerPushListener();
    loadBalance();
    startAutoRefreshTimeline();
    // Lắng nghe notification mới → cập nhật badge ngay lập tức
    NotificationManager.getInstance().getObservableItems()
            .addListener((javafx.collections.ListChangeListener<Object>) c -> updateNotifBadge());
    updateNotifBadge(); // init badge khi mở
    Platform.runLater(() -> {
      if (rootBox != null) {
        ToastManager.init(rootBox);
      }
    });
  }

  // ── Push Listener ─────────────────────────────────────────────────────────
  private void registerPushListener() {
    pushListener = message -> {
      String[] parts = message.split("\\|");
      String header = parts[0];
      switch (header) {

        case Protocol.NOTI_BID_UPDATE: {
          // FIX Bug2b: chỉ cập nhật giá trong currentRows thay vì gọi loadFromServer()
          // để tránh nhiều thread chạy song song khi autobid liên tục
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String amount    = parts[2];
            String bidder    = parts[3];
            try {
              double amt = Double.parseDouble(amount);
              String msg = "🔨 Phiên " + auctionId + " có giá mới: "
                      + String.format("%,.0f", amt) + " VNĐ (bởi " + bidder + ")";
              NotificationManager.getInstance().add(msg, "auction", auctionId);
              // FIX Bug2b: chỉ show toast 1 lần, update card tại chỗ thay vì reload toàn bộ
              // AuctionRow là immutable (all fields final) → không thể cập nhật giá tại chỗ.
              // Dùng loadFromServer() có AtomicBoolean guard để tránh chồng chất thread.
              Platform.runLater(() -> {
                ToastManager.show(ToastManager.Type.INFO, msg);
                loadFromServer();
              });
            } catch (NumberFormatException e) {
              // giá không parse được → mới cần reload
              Platform.runLater(this::loadFromServer);
            }
          }
          break;
        }

        case Protocol.NOTI_BALANCE_CHANGED:
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String newBal    = parts[2];
            String delta     = parts[3];
            Platform.runLater(() -> {
              if (balanceLabel != null) {
                try {
                  double v = Double.parseDouble(newBal);
                  balanceLabel.setText(String.format("%,.0f VNĐ", v));
                } catch (NumberFormatException e) { 
                  balanceLabel.setText(newBal + " VNĐ"); 
                }
              }
              String msg = "🎉 Phiên " + auctionId + " đã kết thúc! Nhận " + delta + " VNĐ";
              NotificationManager.getInstance().add(msg, "auction", auctionId);
              ToastManager.show(ToastManager.Type.SUCCESS, msg);
              loadFromServer();
            });
          } else if (parts.length >= 3) {
            String newBal = parts[2];
            Platform.runLater(() -> {
              if (balanceLabel != null) {
                try {
                  double v = Double.parseDouble(newBal);
                  balanceLabel.setText(String.format("%,.0f VNĐ", v));
                } catch (NumberFormatException e) { 
                  balanceLabel.setText(newBal + " VNĐ"); 
                }
              }
            });
          }
          break;

        case Protocol.NOTI_NEW_AUCTION:
          Platform.runLater(() -> {
            setStatusBar("🆕 Có phiên đấu giá mới! Đang tải lại...");
            ToastManager.show(ToastManager.Type.INFO, "🆕 Có phiên đấu giá mới!");
            loadFromServer();
          });
          break;

        case Protocol.RES_END_SUCCESS: {
          String auctionId = parts.length >= 2 ? parts[1] : "";
          String detail    = parts.length >= 3 ? parts[2] : "";
          boolean isWin = (detail.contains("Winner:" + username)
                  || detail.contains("Winner: " + username))
                  && !"SELLER".equalsIgnoreCase(SessionManager.getInstance().getRole())
                  && !"ADMIN".equalsIgnoreCase(SessionManager.getInstance().getRole());
          if (isWin) {
            String msg = "🎉 Chúc mừng! Bạn đã thắng phiên: " + auctionId;
            NotificationManager.getInstance().add(msg, "auction", auctionId);
            Platform.runLater(() -> ToastManager.show(ToastManager.Type.SUCCESS, msg));
          } else if (!detail.contains("No winner") && !auctionId.isEmpty()) {
            String winnerName = extractWinner(detail);
            boolean isSeller = "SELLER".equalsIgnoreCase(SessionManager.getInstance().getRole());
            String msg = isSeller
                    ? "🏆 Phiên đấu giá " + auctionId + " của bạn đã kết thúc."
                    : "🔔 Phiên " + auctionId + " đã kết thúc. Người thắng: " + winnerName;
            NotificationManager.getInstance().add(msg, "auction", auctionId);
            Platform.runLater(() -> ToastManager.show(ToastManager.Type.INFO, msg));
          } else if (!auctionId.isEmpty()) {
            String msg = "🔔 Phiên " + auctionId + " đã kết thúc. Không có người thắng.";
            NotificationManager.getInstance().add(msg, "auction", auctionId);
            Platform.runLater(() -> ToastManager.show(ToastManager.Type.INFO, msg));
          }
          Platform.runLater(this::loadFromServer);
          break;
        }

        case Protocol.NOTI_SNIPING_UPDATE: {
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String count     = parts[3];
            String msg = "⏱ Phiên " + auctionId + " được gia hạn lần " + count + " (+2 phút)";
            NotificationManager.getInstance().add(msg, "auction", auctionId);
            Platform.runLater(() -> ToastManager.show(ToastManager.Type.WARNING, msg));
          }
          break;
        }

        // FIX Bug1a: xóa phiên bị cancel khỏi list thay vì chỉ loadFromServer
        case Protocol.NOTI_AUCTION_CANCELLED: {
          String cancelledId = parts.length >= 2 ? parts[1] : "";
          String reason = parts.length >= 3 ? parts[2] : "Admin hủy";
          final String finalCancelledId = cancelledId;
          final String finalReason = reason;
          Platform.runLater(() -> {
            // FIX Bug1a: xóa trực tiếp khỏi currentRows và re-render, không cần round-trip server
            currentRows.removeIf(r -> r.getId().equals(finalCancelledId));
            applyFilter();
            setStatusBar("❌ Phiên " + finalCancelledId + ": " + finalReason);
            NotificationManager.getInstance().add(
                    "❌ Phiên " + finalCancelledId + ": " + finalReason + ". Tiền đã được hoàn.",
                    "auction", finalCancelledId);
            ToastManager.show(ToastManager.Type.WARNING,
                    "❌ Phiên " + finalCancelledId + ": " + finalReason);
          });
          break;
        }

        case Protocol.NOTI_OUTBID: {
          if (parts.length >= 4) {
            String auctionId = parts[1];
            String newBidder = parts[2];
            String newAmt    = parts[3];
            biddedAuctions.add(auctionId);
            String detailMsg = "⚠️ Bị vượt giá trong phiên " + auctionId
                    + " — Giá mới: " + newAmt + " VNĐ";
            NotificationManager.getInstance().add(detailMsg, "auction", auctionId);
            // FIX: toast chỉ hiện tên bidder để dedup hoạt động đúng
            Platform.runLater(() -> ToastManager.show(ToastManager.Type.WARNING,
                    "⚠️ Bị vượt giá bởi " + newBidder + "!"));
          }
          break;
        }

        case Protocol.NOTI_REFUND: {
          if (parts.length >= 3) {
            String refundAmt = parts[2];
            String auctionId = parts.length >= 2 ? parts[1] : "";
            String detailMsg = "💰 Hoàn tiền " + refundAmt + " VNĐ vào ví";
            NotificationManager.getInstance().add(detailMsg, "balance", auctionId);
            // FIX: toast cố định để dedup hoạt động, chi tiết vào NotificationManager
            Platform.runLater(() -> 
                ToastManager.show(ToastManager.Type.SUCCESS, "💰 Hoàn tiền vào ví"));
            loadBalance();
          }
          break;
        }

        default:
          break;
      }
    };
    ServerConnection.getInstance().addPushListener(pushListener);
  }

  private void removePushListener() {
    if (pushListener != null) {
      ServerConnection.getInstance().removePushListener(pushListener);
      pushListener = null;
    }
  }

  private String extractWinner(String detail) {
    if (detail == null) { 
      return "N/A";
    }
    int idx = detail.indexOf("Winner:");
    if (idx < 0) {
      idx = detail.indexOf("Winner: ");
    }
    if (idx < 0) {
      return "N/A";
    }
    String rest = detail.substring(idx + 7).trim();
    int end = rest.indexOf("|");
    return end > 0 ? rest.substring(0, end).trim() : rest.trim();
  }

  // ── Load data ─────────────────────────────────────────────────────────────
  private void loadFromServer() {
    // FIX Bug2b: nếu đang load rồi thì bỏ qua request mới
    if (!listLoading.compareAndSet(false, true)) {
      return;
    }

    setStatusBar("Đang tải danh sách phiên...");
    new Thread(() -> {
      try {
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
          if (!conn.connectWithRetry()) {
            Platform.runLater(() -> {
              setStatusBar("❌ Mất kết nối server!");
              AlertUtil.showError("Mất kết nối", "Không thể kết nối server!");
            });
            return;
          }
        }
        String response = conn.sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
        if (response == null || response.startsWith("ERROR")) {
          Platform.runLater(() -> setStatusBar("❌ Mất kết nối server!"));
          return;
        }
        List<AuctionRow> data = new ArrayList<>();
        if (response.startsWith(Protocol.RES_LIST_SUCCESS)) {
          String json = response.substring(
                  Protocol.RES_LIST_SUCCESS.length() + Protocol.SEPARATOR.length());
          AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
          if (rows != null) {
            for (AuctionRow row : rows) {
              // FIX Bug1a: lọc thêm CANCELED/CANCELLED để phiên bị hủy không hiện
              String s = row.getStatus();
              if (!"FINISHED".equals(s) && !"PAID".equals(s)
                      && !"CANCELED".equals(s) && !"CANCELLED".equals(s)) {
                data.add(row);
              }
            }
          }
        }
        final List<AuctionRow> finalData = data;
        Platform.runLater(() -> {
          currentRows.clear();
          currentRows.addAll(finalData);
          applyFilter();
          setStatusBar(finalData.isEmpty()
                  ? "ℹ️ Chưa có phiên nào đang diễn ra."
                  : "✅ Tải xong " + finalData.size() + " phiên.");
        });
      } catch (Exception e) {
        Platform.runLater(() -> setStatusBar("❌ Lỗi tải danh sách!"));
      } finally {
        // FIX Bug2b: luôn release lock
        listLoading.set(false);
      }
    }).start();
  }

  // ── Search ────────────────────────────────────────────────────────────────
  @FXML
  private void handleSearch(KeyEvent e) {
    if (searchField == null) {
      return;
    }
    String keyword = searchField.getText().trim().toLowerCase();
    if (keyword.isEmpty()) {
      applyFilter();
      return;
    }
    List<AuctionRow> filtered = currentRows.stream()
            .filter(r -> r.getItemName().toLowerCase().contains(keyword)
                    || r.getId().toLowerCase().contains(keyword))
            .collect(Collectors.toList());
    renderCards(filtered);
  }

  private static final String FILTER_ACTIVE_STYLE =
          "-fx-background-color: rgba(59,130,246,0.3); -fx-text-fill: #93C5FD;" 
          +
                  "-fx-font-size: 11px; -fx-font-weight: bold;" 
                  +
                  "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 12;" 
                  +
                  "-fx-border-color: #3B82F6; -fx-border-radius: 8; -fx-border-width: 1;";

  private static final String FILTER_INACTIVE_STYLE =
          "-fx-background-color: transparent; -fx-text-fill: #64748B;" 
          +
                  "-fx-font-size: 11px; -fx-background-radius: 8; -fx-cursor: hand;" 
                  +
                  "-fx-padding: 0 12; -fx-border-color: #1E3A5F;" 
                  +
                  "-fx-border-radius: 8; -fx-border-width: 1;";

  @FXML
  private void handleFilterType(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> typeButtons = List.of(btnFilterAll, btnFilterArt,
            btnFilterElec, btnFilterVehicle, btnFilterOther);
    typeButtons.forEach(b -> b.setStyle(FILTER_INACTIVE_STYLE));
    clicked.setStyle(FILTER_ACTIVE_STYLE);
    if (clicked == btnFilterArt) {
      activeTypeFilter = "Art";
    } else if (clicked == btnFilterElec) {
      activeTypeFilter = "Electronics";
    } else if (clicked == btnFilterVehicle) {
      activeTypeFilter = "Vehicle";
    } else if (clicked == btnFilterOther) {
      activeTypeFilter = "OTHER";
    } else {
      activeTypeFilter = "ALL";
    }
    applyFilter();
  }

  @FXML
  private void handleFilterPrice(ActionEvent e) {
    Button clicked = (Button) e.getSource();
    List<Button> priceButtons = List.of(btnPriceAll, btnPriceUnder5, btnPriceMid, btnPriceOver50);
    priceButtons.forEach(b -> b.setStyle(FILTER_INACTIVE_STYLE));
    clicked.setStyle(FILTER_ACTIVE_STYLE);
    if (clicked == btnPriceUnder5) {
      activePriceFilter = "UNDER5";
    } else if (clicked == btnPriceMid) {
      activePriceFilter = "MID";
    } else if (clicked == btnPriceOver50) {
      activePriceFilter = "OVER50";
    } else {
      activePriceFilter = "ALL";
    }
    applyFilter();
  }

  private void applyFilter() {
    String keyword = (searchField != null && searchField.getText() != null)
            ? searchField.getText().trim().toLowerCase() : "";
    List<AuctionRow> filtered = currentRows.stream()
            .filter(r -> keyword.isEmpty()
                    || r.getItemName().toLowerCase().contains(keyword)
                    || r.getId().toLowerCase().contains(keyword))
            .filter(r -> {
              String type = r.getItemType() != null ? r.getItemType() : "";
              return switch (activeTypeFilter) {
                case "Art"         -> "Art".equals(type);
                case "Electronics" -> "Electronics".equals(type);
                case "Vehicle"     -> "Vehicle".equals(type);
                case "OTHER"       -> !List.of("Art", "Electronics", "Vehicle").contains(type);
                default            -> true;
              };
            })
            .filter(r -> switch (activePriceFilter) {
              case "UNDER5" -> r.getCurrentPrice() < 5_000_000;
              case "MID"    -> r.getCurrentPrice() >= 5_000_000 
              && r.getCurrentPrice() <= 50_000_000;
              case "OVER50" -> r.getCurrentPrice() > 50_000_000;
              default       -> true;
            })
            .collect(Collectors.toList());
    renderCards(filtered);
  }

  // ── Card rendering ────────────────────────────────────────────────────────
  private void renderCards(List<AuctionRow> rows) {
    auctionGrid.getChildren().clear();
    selectedRow = null;
    if (rows.isEmpty()) {
      Label empty = new Label("Không có phiên nào phù hợp.");
      empty.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px; -fx-padding: 40;");
      auctionGrid.getChildren().add(empty);
      return;
    }
    for (AuctionRow row : rows) {
      auctionGrid.getChildren().add(buildCard(row));
    }
  }

  private VBox buildCard(AuctionRow row) {
    String statusColor = switch (row.getStatus()) {
      case "RUNNING" -> "#E65100";
      case "OPEN"    -> "#2E7D32";
      default        -> "#888888";
    };
    Label badge = new Label("RUNNING".equals(row.getStatus()) ? "🔴 LIVE" : "⬤ " + row.getStatus());
    badge.setStyle(
            "-fx-background-color: " + statusColor + "22;" 
            +
                    "-fx-text-fill: " + statusColor + ";" 
                    +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" 
                    +
                    "-fx-background-radius: 6; -fx-padding: 3 8;");

    String typeIcon = switch (row.getItemType() != null ? row.getItemType() : "") {
      case "Art"         -> "🎨";
      case "Electronics" -> "⚡";
      case "Vehicle"     -> "🚗";
      default            -> "🏷";
    };

    javafx.scene.Node iconNode;
    String imgUrl = row.getImageUrl();
    if (imgUrl != null && !imgUrl.isEmpty()) {
      javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
      imgView.setFitWidth(187);
      imgView.setFitHeight(120);
      imgView.setPreserveRatio(true);
      imgView.setSmooth(true);
      javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(187, 120);
      clip.setArcWidth(10);
      clip.setArcHeight(10);
      imgView.setClip(clip);
      Label placeholderIcon = new Label(typeIcon);
      placeholderIcon.setStyle("-fx-font-size: 40px;");
      javafx.scene.layout.StackPane imgContainer =
              new javafx.scene.layout.StackPane(placeholderIcon, imgView);
      imgContainer.setPrefWidth(187);
      imgContainer.setPrefHeight(120);
      imgContainer.setStyle("-fx-background-color: #162236; -fx-background-radius: 8;");
      iconNode = imgContainer;
      final String finalImgUrl = imgUrl;
      new Thread(() -> {
        try {
          javafx.scene.image.Image img;
          final double reqW = 187;
          final double reqH = 120;
          if (finalImgUrl.startsWith("data:image")) {
            String base64 = finalImgUrl.substring(finalImgUrl.indexOf(",") + 1);
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes), 
            reqW, reqH, true, true);
          } else {
            java.net.URL url = new java.net.URL(finalImgUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "image/*,*/*");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
            conn.connect();
            try (java.io.InputStream is = conn.getInputStream()) {
              byte[] bytes = is.readAllBytes();
              img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes), reqW, reqH, true, true);
            } finally { conn.disconnect(); }
          }
          if (!img.isError()) {
            final javafx.scene.image.Image finalImg = img;
            javafx.application.Platform.runLater(() -> {
              imgView.setImage(finalImg);
              placeholderIcon.setVisible(false);
            });
          }
        } catch (Exception e) {
          System.err.println("[AuctionList] Không load được ảnh: " + e.getMessage());
        }
      }, "card-img-load").start();
    } else {
      Label icon = new Label(typeIcon);
      icon.setStyle("-fx-font-size: 46px; -fx-padding: 8 0;");
      icon.setMinWidth(187);
      icon.setAlignment(javafx.geometry.Pos.CENTER);
      iconNode = icon;
    }

    Label name = new Label(row.getItemName());
    name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #E2E8F0; -fx-wrap-text: true;");
    name.setMaxWidth(185);
    Label priceLabel = new Label("Giá hiện tại");
    priceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569;");
    Label price = new Label(row.getCurrentPriceFormatted());
    price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #60A5FA;");

    Button btnDetail = new Button("👁 Xem chi tiết");
    btnDetail.setPrefWidth(185);
    btnDetail.setStyle(
            "-fx-background-color: #1565C0; -fx-text-fill: white;" 
            +
                    "-fx-font-size: 11px; -fx-font-weight: bold;" 
                    +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 0;");
    btnDetail.setOnAction(e -> openBidView(row));

    VBox card = new VBox(8, badge, iconNode, name, priceLabel, price, btnDetail);
    card.setPrefWidth(215); card.setMinHeight(265);
    card.setStyle(
            "-fx-background-color: #0B1120;" 
            +
                    "-fx-background-radius: 14;" 
                    +
                    "-fx-border-color: #1E3A5F; -fx-border-radius: 14; -fx-border-width: 1;" 
                    +
                    "-fx-padding: 14; -fx-cursor: hand;");
    card.setOnMouseClicked(e -> {
      auctionGrid.getChildren().forEach(n -> {
        if (n instanceof VBox v) {
          v.setStyle(v.getStyle().replace("-fx-background-color: #162236;", "-fx-background-color: #0B1120;"));
        }
      });
      card.setStyle(card.getStyle().replace("-fx-background-color: #0B1120;", "-fx-background-color: #162236;"));
      selectedRow = row;
    });
    return card;
  }

  private void openBidView(AuctionRow row) {
    stopAutoRefresh();
    removePushListener();
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BidView(stage, row.getId(), row.getItemName(),
            String.valueOf(row.getCurrentPrice()), row.getStatus(),
            username, row.getEndTime(),
            row.getImageUrl(), row.getDescription(),
            row.getItemType(), row.getStartingPrice(), row.getSellerId()).show();
  }

  // ── Watchlist ─────────────────────────────────────────────────────────────
  @FXML
  public void handleWatch() {
    if (selectedRow == null) { setStatusBar("⚠️ Vui lòng click vào một phiên trước!"); return; }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_WATCH + Protocol.SEPARATOR + selectedRow.getId());
      Platform.runLater(() -> {
        if (response != null && response.startsWith(Protocol.RES_WATCH_SUCCESS)) {
          setStatusBar("✅ Đã theo dõi phiên!");
          AlertUtil.showSuccess("Theo dõi thành công", "✅ Đang theo dõi: " + selectedRow.getItemName());
        } else { setStatusBar("❌ Theo dõi thất bại!"); }
      });
    }).start();
  }

  @FXML
  public void handleUnwatch() {
    if (selectedRow == null) { setStatusBar("⚠️ Vui lòng click vào một phiên trước!"); return; }
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
              Protocol.CMD_UNWATCH + Protocol.SEPARATOR + selectedRow.getId());
      Platform.runLater(() -> {
        if (response != null && response.startsWith(Protocol.RES_UNWATCH_SUCCESS)) {
          setStatusBar("✅ Đã bỏ theo dõi!");
        }
        else setStatusBar("❌ Bỏ theo dõi thất bại!");
      });
    }).start();
  }

  @FXML
  public void handleGetWatchlist() {
    stopAutoRefresh();
    removePushListener(); // FIX: dọn listener trước khi rời màn để tránh toast lặp ở Watchlist
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new WatchlistView(stage, username).show();
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  private void setStatusBar(String msg) { if (statusBarLabel != null) {
      statusBarLabel.setText(msg);
    } }

  @FXML public void handleRefresh() { loadFromServer(); }
  public void refreshList()         { loadFromServer(); }

  private void startAutoRefreshTimeline() {
    autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> loadFromServer()));
    autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    autoRefreshTimeline.play();
  }

  private void stopAutoRefresh() {
    if (autoRefreshTimeline != null) { autoRefreshTimeline.stop(); autoRefreshTimeline = null; }
  }

  @FXML
  public void handleLogout() {
    stopAutoRefresh();
    removePushListener();
    ServerConnection.getInstance().disconnect();
    SessionManager.getInstance().clear();
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new LoginView(stage).show();
  }

  @FXML public void handleCreateAuction() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new CreateAuctionView(stage, username).show();
  }

  @FXML public void handleAdminDashboard() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new AdminDashboardView(stage, username).show();
  }

  @FXML public void handleSellerDashboard() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new SellerView(stage, username).show();
  }

  @FXML
  private void loadBalance() {
    if (balanceLabel == null) {
      return;
    }
    new Thread(() -> {
      String res = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_GET_BALANCE);
      Platform.runLater(() -> {
        if (res != null && res.startsWith(Protocol.RES_BALANCE_INFO)) {
          String[] p = res.split("\\|", -1);
          String amt = p.length >= 2 ? p[1] : "---";
          try {
            double v = Double.parseDouble(amt);
            if (balanceLabel != null) {
              balanceLabel.setText(String.format("%,.0f VNĐ", v));
            }
          } catch (NumberFormatException e) {
            if (balanceLabel != null) {
              balanceLabel.setText(amt + " VNĐ");
            }
          }
        } else { if (balanceLabel != null) {
            balanceLabel.setText("---");
          } }
      });
    }, "auctionlist-balance-thread").start();
  }

  public void handleBalance() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BalanceView(stage, username).show();
  }

  private void updateNotifBadge() {
    if (notifBadge == null) {
      return;
    }
    long count = NotificationManager.getInstance().unreadCount();
    Platform.runLater(() -> {
      if (count > 0) {
        notifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        notifBadge.setVisible(true);
        notifBadge.setManaged(true);
      } else {
        notifBadge.setVisible(false);
        notifBadge.setManaged(false);
      }
    });
  }

  @FXML public void handleNotification() {
    // Ẩn badge khi user mở notification
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new NotificationView(stage, username).show();
    updateNotifBadge();
  }

  @FXML public void handleBidHistory() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new BidHistoryView(stage, username).show();
  }

  @FXML public void handleProfile() {
    Stage stage = (Stage) auctionGrid.getScene().getWindow();
    new ProfileView(stage, username).show();
  }

  @FXML public void handleToggleSidebar() {}
}