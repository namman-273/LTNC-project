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

    public enum Type { SUCCESS, WARNING, INFO, DANGER }

    private static VBox container;

    public static void init(StackPane root) {
        container = new VBox(8);
        container.setAlignment(Pos.TOP_RIGHT);
        container.setPadding(new Insets(16, 16, 0, 0));
        container.setPickOnBounds(false);
        container.setMaxWidth(320);
        StackPane.setAlignment(container, Pos.TOP_RIGHT);
        root.getChildren().add(container);
    }

    public static void show(Type type, String message) {
        if (container == null) return;
        Platform.runLater(() -> {
            HBox toast = buildToast(type, message);
            toast.setOpacity(0);
            container.getChildren().add(0, toast);

            // Fade in
            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(toast.opacityProperty(), 0)),
                    new KeyFrame(Duration.millis(250), new KeyValue(toast.opacityProperty(), 1))
            );

            // Fade out sau 3.5s
            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(toast.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(300), new KeyValue(toast.opacityProperty(), 0))
            );
            fadeOut.setDelay(Duration.seconds(3.5));
            fadeOut.setOnFinished(e -> container.getChildren().remove(toast));

            fadeIn.play();
            fadeOut.play();
        });
    }

    private static HBox buildToast(Type type, String message) {
        String bg, border, color, icon;
        switch (type) {
            case SUCCESS: bg="#F0FFF4"; border="#A5D6A7"; color="#1B5E20"; icon="✅"; break;
            case WARNING: bg="#FFF8E1"; border="#FFE082"; color="#E65100"; icon="⚠️"; break;
            case DANGER:  bg="#FFEBEE"; border="#EF9A9A"; color="#B71C1C"; icon="❌"; break;
            default:      bg="#E3F2FD"; border="#90CAF9"; color="#0D47A1"; icon="💡"; break;
        }

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color
                + "; -fx-wrap-text: true;");
        msgLabel.setMaxWidth(260);

        HBox toast = new HBox(8, iconLabel, msgLabel);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(10, 14, 10, 14));
        toast.setStyle(
                "-fx-background-color: " + bg + ";"
                        + "-fx-border-color: " + border + ";"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 10;"
                        + "-fx-background-radius: 10;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);"
        );
        return toast;
    }
}