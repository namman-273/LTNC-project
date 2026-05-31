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
import javafx.util.StringConverter;

/**
 * Controller màn tạo phiên đấu giá.
 */
public class CreateAuctionController implements Initializable {

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

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    typeComboBox.setItems(FXCollections.observableArrayList(
        "ART", "ELECTRONICS", "VEHICLE", "OTHER"));
    typeComboBox.setConverter(new ItemTypeConverter());
    typeComboBox.getSelectionModel().selectFirst();
  }

  // ── Create flow ────────────────────────────────────────────────────────────
  @FXML
  private void handleCreate() {
    FormData form = collectForm(); // SRP
    showMessage("Đang gửi yêu cầu...", null);

    new Thread(() -> {
      ServerConnection conn = ServerConnection.getInstance();
      if (!conn.isConnected()) {
        Platform.runLater(() -> showMessage("Mất kết nối server!", false));
        return;
      }
      String response = conn.sendAndReceive(buildCommand(form)); // SRP
      System.out.println("Create auction response: " + response);
      Platform.runLater(() -> {
        if (response == null) {
          showMessage("Mất kết nối server!", false);
          return;
        }
        String[] parts = response.split("\\" + Protocol.SEPARATOR);
        if (response.startsWith(Protocol.RES_SUCCESS)) {
          handleCreateSuccess(parts); // SRP
        } else {
          handleCreateFailure(parts); // SRP
        }
      });
    }, "create-auction-thread").start();
  }

  @FXML
  private void handleBack() {
    Stage stage = (Stage) nameField.getScene().getWindow();
    new AuctionListView(stage, username).show();
  }

  // ── SRP helpers ────────────────────────────────────────────────────────────

  /** Thu thập toàn bộ giá trị từ form vào một record bất biến. */
  private FormData collectForm() {
    return new FormData(
        typeComboBox.getValue() != null ? typeComboBox.getValue() : "OTHER",
        nameField.getText().trim(),
        priceField.getText().trim(),
        durationField.getText().trim(),
        descriptionArea != null ? descriptionArea.getText().trim() : "",
        imageUrlField != null ? imageUrlField.getText().trim() : "");
  }

  /** Tạo chuỗi lệnh gửi lên server — BE expect format này. */
  private String buildCommand(FormData f) {
    return Protocol.CMD_CREATE_AUCTION + Protocol.SEPARATOR
        + f.type + Protocol.SEPARATOR
        + f.name + Protocol.SEPARATOR
        + f.price + Protocol.SEPARATOR
        + f.duration + Protocol.SEPARATOR
        + f.description + Protocol.SEPARATOR
        + f.imageUrl;
  }

  private void handleCreateSuccess(String[] parts) {
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
    }, "create-redirect-thread").start();
  }

  private void handleCreateFailure(String[] parts) {
    showMessage(parts.length > 1 ? parts[1] : "Tạo phiên thất bại!", false);
  }

  /**
   * DRY: thay thế 2 bản copy showError / showSuccess.
   *
   * @param success true = xanh, false = đỏ, null = xám
   */
  private void showMessage(String msg, Boolean success) {
    if (success == null) {
      messageLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
    } else if (success) {
      messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
    } else {
      messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }
    messageLabel.setText(msg);
  }

  // ── Inner types ────────────────────────────────────────────────────────────

  /** Value object bất biến giữ dữ liệu form — tránh truyền 6 tham số rời. */
  private record FormData(
      String type, String name, String price,
      String duration, String description, String imageUrl) {
  }

  /**
   * OCP: thêm item type mới chỉ cần thêm case ở đây.
   * Không ảnh hưởng bất kỳ logic nào khác.
   */
  private static class ItemTypeConverter extends StringConverter<String> {
    @Override
    public String toString(String s) {
      if (s == null) {
        return "";
      }
      return switch (s) {
        case "ART" -> "🎨 Nghệ thuật";
        case "ELECTRONICS" -> "⚡ Điện tử";
        case "VEHICLE" -> "🚗 Xe cộ";
        default -> "🏷 Khác";
      };
    }

    @Override
    public String fromString(String s) {
      return s; // ComboBox dùng value gốc để gửi lên server
    }
  }
}