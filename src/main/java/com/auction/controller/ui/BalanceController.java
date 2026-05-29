package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.views.java.AuctionListView;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Balancecontroller.
 */
public class BalanceController extends BaseController implements Initializable {

  @FXML
  private Label balanceLabel;
  @FXML
  private Label usernameLabel;
  @FXML
  private Label messageLabel;
  @FXML
  private TextField depositAmountField;
  @FXML
  private ListView<TransactionItem> transactionList;

  private String username;
  private static final ObservableList<TransactionItem> transactions = FXCollections.observableArrayList();
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private Timeline autoRefreshTimeline;

  // ── Transaction DTO ───────────────────────────────────────────────────────
  /**
 * transaction dto.
 */
  public static class TransactionItem {
    final String amount;
    final String type;
    final String time;
    final boolean success;

    TransactionItem(String amount, String type, String time, boolean success) {
      this.amount = amount;
      this.type = type;
      this.time = time;
      this.success = success;
    }
  }

  // ── Custom Cell ──────────────────────────────────────────────────────────
  private static class TransactionCell extends ListCell<TransactionItem> {
    private final HBox card = new HBox(12);
    private final Label iconLabel = new Label();
    private final VBox content = new VBox(3);
    private final Label amountLabel = new Label();
    private final HBox bottomRow = new HBox(8);
    private final Label typeLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label statusBadge = new Label();

    TransactionCell() {
      iconLabel.setPrefSize(36, 36);
      iconLabel.setAlignment(Pos.CENTER);
      iconLabel.setStyle("-fx-font-size: 20px; -fx-background-radius: 18;"
          + "-fx-min-width: 36; -fx-min-height: 36; -fx-alignment: CENTER;");
      amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
      typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
      timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
      statusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
          + "-fx-background-radius: 10; -fx-padding: 1 7;");
      bottomRow.setAlignment(Pos.CENTER_LEFT);
      bottomRow.getChildren().addAll(typeLabel, timeLabel, statusBadge);
      content.getChildren().addAll(amountLabel, bottomRow);
      HBox.setHgrow(content, Priority.ALWAYS);
      card.setAlignment(Pos.CENTER_LEFT);
      card.setPadding(new Insets(12, 14, 12, 14));
      card.getChildren().addAll(iconLabel, content);
      setStyle("-fx-background-color: transparent; -fx-padding: 0;");
      setText(null);
    }

    @Override
    protected void updateItem(TransactionItem item, boolean empty) {
      super.updateItem(item, empty);
      setStyle("-fx-background-color: transparent; -fx-padding: 0;");
      if (empty || item == null) {
        setGraphic(null);
        return;
      }

      iconLabel.setText(item.success ? "💳" : "❌");
      iconLabel.setStyle("-fx-font-size: 18px; -fx-background-color: "
          + (item.success ? "rgba(59,130,246,0.15)" : "rgba(248,113,113,0.12)")
          + "; -fx-background-radius: 18;"
          + "-fx-min-width: 40; -fx-min-height: 40; -fx-alignment: CENTER;");
      amountLabel.setText(item.amount);
      amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
          + (item.success ? "#60A5FA" : "#F87171") + ";");
      typeLabel.setText(item.type + "  •  ");
      timeLabel.setText(item.time + "  •  ");
      statusBadge.setText(item.success ? "✓ Thành công" : "✗ Thất bại");
      statusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;"
          + "-fx-background-radius: 10; -fx-padding: 1 7;"
          + "-fx-background-color: "
          + (item.success ? "rgba(52,211,153,0.15)" : "rgba(248,113,113,0.15)") + "; "
          + "-fx-text-fill: " + (item.success ? "#34D399" : "#F87171") + ";");
      card.setStyle("-fx-background-color: "
          + (item.success ? "rgba(59,130,246,0.06)" : "rgba(30,42,64,0.5)")
          + "; -fx-background-radius: 12;"
          + "-fx-border-color: transparent transparent transparent "
          + (item.success ? "#3B82F6" : "#EF4444")
          + "; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 12 12 0;");

      VBox outer = new VBox(card);
      outer.setPadding(new Insets(0, 0, 7, 0));
      setGraphic(outer);
      setText(null);
    }
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  /**
 * set users names.
 */
  public void setUsername(String username) {
    initToastManager(balanceLabel); // BaseController — loại bỏ bản copy
    this.username = username;
    usernameLabel.setText("Tài khoản: " + username);
    loadBalanceInternal();
    startAutoRefresh();
    // Lắng nghe NOTI_BALANCE_CHANGED để cập nhật ngay thay vì chờ poll 10 giây
    registerPushListener(this::handlePushMessage); // BaseController
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    if (transactionList != null) {
      transactionList.setItems(transactions);
      transactionList.setCellFactory(lv -> new TransactionCell());
    }
  }

  // ── Load số dư (dùng nội bộ vì cần set style riêng) ─────────────────────
  // BaseController.loadBalance chỉ set text; BalanceController cần set thêm
  // font-size 26px + text-fill → override bằng method riêng.
  private void loadBalanceInternal() {
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_GET_BALANCE);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_BALANCE_INFO) && parts.length > 1) {
          try {
            double balance = Double.parseDouble(parts[1]);
            balanceLabel.setText(String.format("%,.0f VNĐ", balance));
            balanceLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #60A5FA;");
          } catch (NumberFormatException e) {
            balanceLabel.setText(parts[1]);
          }
        } else {
          showMessage("❌ " + (parts.length > 1 ? parts[1] : "Lỗi tải số dư!"), "red");
        }
      });
    }).start();
  }

  // ── Push ──────────────────────────────────────────────────────────────────
  private void handlePushMessage(String message) {
    String[] parts = message.split("\\|");
    if (parts.length >= 2 && Protocol.NOTI_BALANCE_CHANGED.equals(parts[0])) {
      String newBal = parts[1];
      Platform.runLater(() -> {
        try {
          double v = Double.parseDouble(newBal);
          balanceLabel.setText(String.format("%,.0f VNĐ", v));
          balanceLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #60A5FA;");
        } catch (NumberFormatException ignored) {
          ignored.printStackTrace();
        }
      });
    }
  }

  // ── Quick-fill buttons ────────────────────────────────────────────────────
  @FXML
  private void handleQuick100K() {
    depositAmountField.setText("100000");
  }

  @FXML
  private void handleQuick500K() {
    depositAmountField.setText("500000");
  }

  @FXML
  private void handleQuick1M() {
    depositAmountField.setText("1000000");
  }

  @FXML
  private void handleQuick5M() {
    depositAmountField.setText("5000000");
  }

  @FXML
  private void handleDeposit() {
    String amount = depositAmountField.getText().trim();
    if (amount.isEmpty()) {
      showMessage("Vui lòng nhập số tiền!", "orange");
      return;
    }

    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_DEPOSIT + Protocol.SEPARATOR + amount);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        String time = LocalDateTime.now().format(FORMATTER);
        if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
          String msg = parts.length > 2 ? parts[2] : "Nạp tiền thành công!";
          showMessage("✅ " + msg, "green");
          depositAmountField.clear();
          try {
            double amt = Double.parseDouble(amount);
            transactions.add(0, new TransactionItem(
                "+" + String.format("%,.0f VNĐ", amt), "Nạp tiền", time, true));
          } catch (NumberFormatException ignored) {
            ignored.printStackTrace();
          }
          loadBalanceInternal();
        } else {
          String msg = parts.length > 1 ? parts[1] : "Nạp tiền thất bại!";
          showMessage("❌ " + msg, "red");
          try {
            double amt = Double.parseDouble(amount);
            transactions.add(0, new TransactionItem(
                String.format("%,.0f VNĐ", amt), "Nạp tiền", time, false));
          } catch (NumberFormatException ignored) {
            ignored.printStackTrace();
          }
        }
      });
    }).start();
  }

  @FXML
  private void handleRefresh() {
    loadBalanceInternal();
  }

  @FXML
  private void startAutoRefresh() {
    autoRefreshTimeline = new Timeline(
        new KeyFrame(Duration.seconds(10), e -> loadBalanceInternal()));
    autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    autoRefreshTimeline.play();
  }

  private void stopAutoRefresh() {
    if (autoRefreshTimeline != null) {
      autoRefreshTimeline.stop();
      autoRefreshTimeline = null;
    }
  }

  @FXML
  private void handleBack() {
    stopAutoRefresh();
    removePushListener(); // BaseController
    Stage stage = (Stage) balanceLabel.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  private void showMessage(String msg, String color) {
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }
}