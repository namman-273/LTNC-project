package com.auction.util.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastManager {

  public enum Type {
    SUCCESS, WARNING, INFO, DANGER
  }

  private static VBox container;
  private static javafx.scene.layout.StackPane initializedRoot;

  // FIX: dedup guard — lưu message đang hiện, bỏ qua nếu trùng trong 1.5 giây
  private static final java.util.Map<String, Long> recentToasts =
          new java.util.concurrent.ConcurrentHashMap<>();
  private static final long DEDUP_WINDOW_MS = 1500;

  public static void init(javafx.scene.layout.StackPane root) {
    // Nếu đã init trên đúng root này rồi thì không làm gì
    if (root == initializedRoot && container != null) return;
    // Xóa container cũ khỏi root cũ nếu có
    if (container != null && initializedRoot != null) {
      initializedRoot.getChildren().remove(container);
    }
    initializedRoot = root;
    container = new VBox(8);
    container.setAlignment(Pos.TOP_RIGHT);
    container.setPadding(new Insets(16, 16, 0, 0));
    container.setPickOnBounds(false);
    container.setMaxWidth(340);
    StackPane.setAlignment(container, Pos.TOP_RIGHT);
    root.getChildren().add(container);
  }

  public static void show(Type type, String message) {
    if (container == null)
      return;
    // FIX: bỏ qua toast trùng trong vòng DEDUP_WINDOW_MS ms
    String key = type.name() + "|" + message;
    long now = System.currentTimeMillis();
    Long lastShown = recentToasts.get(key);
    if (lastShown != null && (now - lastShown) < DEDUP_WINDOW_MS) return;
    recentToasts.put(key, now);
    // Dọn entry cũ để tránh map phình to
    recentToasts.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_WINDOW_MS * 4);

    Platform.runLater(() -> {
      HBox toast = buildToast(type, message);
      toast.setOpacity(0);
      container.getChildren().add(0, toast);

      Timeline fadeIn = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(toast.opacityProperty(), 0)),
              new KeyFrame(Duration.millis(220), new KeyValue(toast.opacityProperty(), 1)));
      Timeline fadeOut = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(toast.opacityProperty(), 1)),
              new KeyFrame(Duration.millis(300), new KeyValue(toast.opacityProperty(), 0)));
      fadeOut.setDelay(Duration.seconds(3.5));
      fadeOut.setOnFinished(e -> container.getChildren().remove(toast));

      fadeIn.play();
      fadeOut.play();
    });
  }

  private static HBox buildToast(Type type, String message) {
    String bg, borderLeft, textColor, iconText, iconBg;
    switch (type) {
      case SUCCESS:
        bg = "#F0FFF4";
        borderLeft = "#22C55E";
        textColor = "#14532D";
        iconText = "✔";
        iconBg = "#22C55E";
        break;
      case WARNING:
        bg = "#FFFBEB";
        borderLeft = "#F59E0B";
        textColor = "#78350F";
        iconText = "▲";
        iconBg = "#F59E0B";
        break;
      case DANGER:
        bg = "#FFF1F2";
        borderLeft = "#EF4444";
        textColor = "#7F1D1D";
        iconText = "✖";
        iconBg = "#EF4444";
        break;
      default: // INFO
        bg = "#EFF6FF";
        borderLeft = "#3B82F6";
        textColor = "#1E3A5F";
        iconText = "●";
        iconBg = "#3B82F6";
        break;
    }

    // Icon circle
    Label iconLabel = new Label(iconText);
    iconLabel.setStyle(
            "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: white;");

    StackPane iconCircle = new StackPane(iconLabel);
    iconCircle.setPrefSize(26, 26);
    iconCircle.setMinSize(26, 26);
    iconCircle.setMaxSize(26, 26);
    iconCircle.setStyle(
            "-fx-background-color: " + iconBg + ";" +
                    "-fx-background-radius: 13;");

    // Message
    Label msgLabel = new Label(message);
    msgLabel.setStyle(
            "-fx-font-size: 12px;" +
                    "-fx-text-fill: " + textColor + ";" +
                    "-fx-wrap-text: true;");
    msgLabel.setMaxWidth(270);
    msgLabel.setWrapText(true);

    HBox toast = new HBox(10, iconCircle, msgLabel);
    toast.setAlignment(Pos.CENTER_LEFT);
    toast.setPadding(new Insets(10, 16, 10, 12));
    toast.setMaxWidth(340);
    toast.setStyle(
            "-fx-background-color: " + bg + ";" +
                    "-fx-border-color: transparent transparent transparent " + borderLeft + ";" +
                    "-fx-border-width: 0 0 0 4;" +
                    "-fx-background-radius: 10;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);");
    return toast;
  }
}
