package com.auction.controller.ui;

import com.auction.model.dto.AuctionRow;
import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.NotificationManager;
import com.auction.util.ui.ToastManager;
import com.auction.views.java.AuctionListView;
import com.auction.views.java.CreateAuctionView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * admindashboardcontroller.
 */
public class AdminDashboardController extends BaseController implements Initializable {

  @FXML
  private TableView<AuctionRow> auctionTable;
  @FXML
  private TableColumn<AuctionRow, String> idCol;
  @FXML
  private TableColumn<AuctionRow, String> nameCol;
  @FXML
  private TableColumn<AuctionRow, String> priceCol;
  @FXML
  private TableColumn<AuctionRow, String> statusCol;
  @FXML
  private Label messageLabel;
  @FXML
  private Label balanceLabel;
  @FXML
  private Label statTotalLabel;
  @FXML
  private Label statOpenLabel;
  @FXML
  private Label statFinishedLabel;
  @FXML
  private TextField depositAmountField;
  @FXML
  private TextField depositUsernameField;

  private String username;
  private Timeline autoRefreshTimeline;

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(java.time.LocalDateTime.class,
          (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) 
           (json, type, ctx) -> java.time.LocalDateTime
              .parse(json.getAsString()))
      .create();

  public void setUsername(String username) {
    this.username = username;
    loadBalanceWithPrefix(balanceLabel, "Số dư: ");
  }

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    nameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPriceFormatted"));
    statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    statusCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(String status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
          setText(null);
          setStyle("");
          return;
        }
        setText(status);
        switch (status) {
          case "OPEN" -> setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
          case "RUNNING" -> setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
          case "FINISHED" -> setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
          default -> setStyle("-fx-text-fill: #888888;");
        }
      }
    });

    loadFromServer();
    startAutoRefresh();
    registerPushListener(this::handlePushMessage);
    initToastManager(auctionTable);
  }

  private void handlePushMessage(String message) {
    String[] parts = message.split("\\|");
    if (parts.length == 0) {
      return;
    }

    if (Protocol.NOTI_AUCTION_CANCELLED.equals(parts[0])) {
      String cancelledId = parts.length >= 2 ? parts[1] : "";
      String detail = parts.length >= 3 ? parts[2] : "Phiên đã bị hủy";
      Platform.runLater(() -> {
        if (!cancelledId.isEmpty() && auctionTable.getItems() != null) {
          auctionTable.getItems().removeIf(r -> cancelledId.equals(r.getId()));
          updateStatLabels(auctionTable.getItems());
        }
        showMessage("🚫 Phiên " + cancelledId + " bị hủy: " + detail, "gray");
      });
    }
  }

  /**
   * loadfromserver.
   */
  public void loadFromServer() {
    showMessage("Đang tải danh sách...", "gray");
    new Thread(() -> {
      String response = ServerConnection.getInstance()
          .sendAndReceive(Protocol.CMD_LIST_AUCTIONS);
      ObservableList<AuctionRow> data = FXCollections.observableArrayList();
      if (response != null && response.startsWith(Protocol.RES_LIST_SUCCESS)) {
        String json = response.substring(
            Protocol.RES_LIST_SUCCESS.length() + Protocol.SEPARATOR.length());
        AuctionRow[] rows = gson.fromJson(json, AuctionRow[].class);
        if (rows != null) {
          data.addAll(rows);
        }
      }
      final ObservableList<AuctionRow> finalData = data;
      Platform.runLater(() -> {
        auctionTable.setItems(finalData);
        updateStatLabels(finalData);
        showMessage(finalData.isEmpty()
            ? "ℹ️ Chưa có phiên nào."
            : "✅ Tải xong " + finalData.size() + " phiên.", "gray");
      });
    }).start();
  }

  /** SRP: tách riêng việc cập nhật stat labels. */
  private void updateStatLabels(ObservableList<AuctionRow> items) {
    long openCount = items.stream()
        .filter(r -> "OPEN".equals(r.getStatus()) || "RUNNING".equals(r.getStatus()))
        .count();
    long finishedCount = items.stream()
        .filter(r -> AuctionUtils.isFinishedStatus(r.getStatus()))
        .count();
    if (statTotalLabel != null) {
      statTotalLabel.setText(String.valueOf(items.size()));
    }
    if (statOpenLabel != null) {
      statOpenLabel.setText(String.valueOf(openCount));
    }
    if (statFinishedLabel != null) {
      statFinishedLabel.setText(String.valueOf(finishedCount));
    }
  }

  /**
   * handle.
   */
  @FXML
  public void handleDeposit() {
    String amount = depositAmountField != null ? depositAmountField.getText().trim() : "";
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_DEPOSIT + Protocol.SEPARATOR + amount);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_DEPOSIT_SUCCESS)) {
          showMessage("✅ " + (parts.length > 2 ? parts[2] : "Nạp tiền thành công!"), "green");
          if (depositAmountField != null) {
            depositAmountField.clear();
          }
          loadBalanceWithPrefix(balanceLabel, "Số dư: ");
        } else {
          showMessage("❌ " + (parts.length > 1 ? parts[1] : "Nạp tiền thất bại!"), "red");
        }
      });
    }).start();
  }

  @FXML
  public void handleRefresh() {
    loadFromServer();
  }

  /**
   * .
   */
  @FXML
  public void handleEndAuction() {
    AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên để kết thúc!", "red");
      return;
    }
    final String endAuctionId = selected.getId();
    final String endAuctionName = selected.getItemName();
    showMessage("Đang kết thúc phiên...", "orange");
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_END_AUCTION + Protocol.SEPARATOR + endAuctionId);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        if (response.startsWith(Protocol.RES_ADMIN_END_SUCCESS)) {
          showMessage("✅ Đã đóng phiên " + endAuctionName, "green");
          NotificationManager.getInstance().add(
              "⚠️ [Admin] Đã đóng sớm phiên: " + endAuctionName + " (" + endAuctionId + ")",
              "system", endAuctionId);
          ToastManager.show(ToastManager.Type.SUCCESS, "✅ Đã đóng phiên " + endAuctionName);
        } else {
          String[] parts = response.split("\\" + Protocol.SEPARATOR);
          String msg = parts.length > 1 ? parts[1] : "Lỗi kết thúc phiên!";
          showMessage("❌ " + msg, "red");
          ToastManager.show(ToastManager.Type.DANGER, "❌ " + msg);
        }
        startAutoRefresh();
        loadFromServer();
      });
    }).start();
  }

  /**
   * .
   */
  @FXML
  public void handleDeleteAuction() {
    AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên để xóa!", "red");
      return;
    }

    javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
        javafx.scene.control.Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận xóa");
    confirm.setHeaderText(null);
    confirm.setContentText("Bạn có chắc muốn xóa phiên:\n"
        + selected.getItemName() + "?\nHành động này không thể hoàn tác!");
    java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
      return;
    }

    final String delAuctionId = selected.getId();
    final String delAuctionName = selected.getItemName();
    showMessage("Đang xóa phiên...", "orange");
    new Thread(() -> {
      String response = ServerConnection.getInstance().sendAndReceive(
          Protocol.CMD_DELETE_AUCTION + Protocol.SEPARATOR + delAuctionId);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", "red");
          return;
        }
        if (response.startsWith(Protocol.RES_DELETE_SUCCESS)) {
          showMessage("✅ Đã xóa phiên " + delAuctionName, "green");
          NotificationManager.getInstance().add(
              "🗑️ [Admin] Đã xóa phiên: " + delAuctionName + " (" + delAuctionId + ")",
              "system", delAuctionId);
          ToastManager.show(ToastManager.Type.SUCCESS, "🗑️ Đã xóa phiên " + delAuctionName);
        } else {
          String[] parts = response.split("\\" + Protocol.SEPARATOR);
          String msg = parts.length > 1 ? parts[1] : "Xóa phiên thất bại!";
          showMessage("❌ " + msg, "red");
          ToastManager.show(ToastManager.Type.DANGER, "❌ " + msg);
        }
        loadFromServer();
      });
    }).start();
  }

  /**
   * .
   */
  @FXML
  public void handleCreateAuction() {
    stopAutoRefresh();
    Stage stage = (Stage) auctionTable.getScene().getWindow();
    new CreateAuctionView(stage, username).show();
  }

  @FXML
  private void startAutoRefresh() {
    autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> loadFromServer()));
    autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    autoRefreshTimeline.play();
  }

  private void stopAutoRefresh() {
    if (autoRefreshTimeline != null) {
      autoRefreshTimeline.stop();
      autoRefreshTimeline = null;
    }
  }

  /**
   * .
   */
  public void handleBack() {
    stopAutoRefresh();
    removePushListener();
    Stage stage = (Stage) auctionTable.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  private void showMessage(String msg, String color) {
    if (messageLabel != null) {
      messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
      messageLabel.setText(msg);
    }
  }
}