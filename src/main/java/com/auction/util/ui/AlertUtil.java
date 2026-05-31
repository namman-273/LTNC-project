package com.auction.util.ui;

import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/**
 * Utility class hiển thị thông báo dialog cho user.
 */
public class AlertUtil {

  /**
   * Hiện dialog lỗi.
   */
  public static void showError(String title, String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  /**
   * Hiện dialog thông báo thành công.
   */
  public static void showSuccess(String title, String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  /**
   * Hiện dialog cảnh báo mất kết nối với nút Retry.
   * Trả về true nếu user bấm Retry, false nếu Cancel.
   */
  public static boolean showConnectionLost(String context) {
    final boolean[] retry = { false };
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.WARNING);
      alert.setTitle("Mất kết nối server");
      alert.setHeaderText("Không thể kết nối đến server!");
      alert.setContentText("Mất kết nối khi " + context + ".\nBạn có muốn thử lại không?");

      ButtonType retryBtn = new ButtonType("🔄 Thử lại");
      ButtonType cancelBtn = new ButtonType("❌ Huỷ");
      alert.getButtonTypes().setAll(retryBtn, cancelBtn);

      Optional<ButtonType> result = alert.showAndWait();
      retry[0] = result.isPresent() && result.get() == retryBtn;
    });
    return retry[0];
  }

  /**
   * Hiện dialog xác nhận.
   * Trả về true nếu user bấm OK.
   */
  public static boolean showConfirm(String title, String message) {
    final boolean[] confirmed = { false };
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);

      Optional<ButtonType> result = alert.showAndWait();
      confirmed[0] = result.isPresent() && result.get() == ButtonType.OK;
    });
    return confirmed[0];
  }
}