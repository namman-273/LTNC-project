package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.views.java.AuctionListView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller xử lý luồng tạo phiên đấu giá mới.
 */
public class CreateAuctionController extends BaseController implements Initializable {

  @FXML
  private ComboBox<String> typeComboBox;
  @FXML
  private TextField nameField;
  @FXML
  private TextField priceField;
  @FXML
  private TextField durationField;
  @FXML
  private TextField imageUrlField;
  @FXML
  private TextArea descriptionArea;
  @FXML
  private Label messageLabel;

  private String username;

  public void setUsername(String username) {
    this.username = username;
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    typeComboBox.setItems(FXCollections.observableArrayList(
        "ART", "ELECTRONICS", "VEHICLE", "OTHER"));
    // Hiển thị nhãn tiếng Việt để tránh chọn nhầm
    typeComboBox.setConverter(new javafx.util.StringConverter<String>() {
      @Override
      public String toString(String s) {
        if (s == null)
          return "";
        return switch (s) {
          case "ART" -> "🎨 Nghệ thuật";
          case "ELECTRONICS" -> "⚡ Điện tử";
          case "VEHICLE" -> "🚗 Xe cộ";
          default -> "🏷 Khác";
        };
      }

      @Override
      public String fromString(String s) {
        return s;
      }
    });
    typeComboBox.getSelectionModel().selectFirst();
  }

  // ── Handlers ──────────────────────────────────────────────────────────────

  @FXML
  private void handleCreate() {
    String type = typeComboBox.getValue();
    String name = nameField.getText().trim();
    String price = priceField.getText().trim();
    String duration = durationField.getText().trim();
    String description = descriptionArea != null ? descriptionArea.getText().trim() : "";
    String imageUrl = imageUrlField != null ? imageUrlField.getText().trim() : "";

    // Không tự validate — gửi thẳng lên BE
    // BE expect: CREATE_AUCTION|type|name|price|duration|description|imageUrl
    new Thread(() -> {
      ServerConnection conn = ServerConnection.getInstance();
      if (!conn.isConnected()) {
        Platform.runLater(() -> showMessage("Mất kết nối server!", false));
        return;
      }

      String response = conn.sendAndReceive(
          Protocol.CMD_CREATE_AUCTION + Protocol.SEPARATOR
              + type + Protocol.SEPARATOR
              + name + Protocol.SEPARATOR
              + price + Protocol.SEPARATOR
              + duration + Protocol.SEPARATOR
              + description + Protocol.SEPARATOR
              + imageUrl);
      System.out.println("Create auction response: " + response);

      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", false);
          return;
        }

        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_SUCCESS)) {
          String msg = parts.length > 1 ? parts[1] : "Tạo phiên thành công!";
          showMessage(msg + " Đang chuyển về danh sách...", true);
          new Thread(() -> {
            try {
              Thread.sleep(1500);
              Platform.runLater(() -> {
                Stage stage = (Stage) nameField.getScene().getWindow();
                new AuctionListView(stage, username).show();
              });
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }, "create-auction-navigate-thread").start();
        } else {
          String errorMsg = parts.length > 1 ? parts[1] : "Tạo phiên thất bại!";
          showMessage(errorMsg, false);
        }
      });
    }, "create-auction-thread").start();
  }

  @FXML
  private void handleBack() {
    Stage stage = (Stage) nameField.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  // ── UI helper ──────────────────────────────────────────────────────────────

  /** SRP: một điểm duy nhất hiển thị kết quả thao tác. */
  private void showMessage(String msg, boolean success) {
    messageLabel.setStyle(success
        ? "-fx-text-fill: green; -fx-font-size: 12px;"
        : "-fx-text-fill: red;   -fx-font-size: 12px;");
    messageLabel.setText(msg);
  }
}