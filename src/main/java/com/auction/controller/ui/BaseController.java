package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.ui.ToastManager;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;


/**
 * Base controller chứa các hàm dùng chung cho toàn bộ controller trong package ui.
 *
 * 
 */
public abstract class BaseController {

 
  protected Consumer<String> pushListener;

  protected void initToastManager(Node anchor) {
    Platform.runLater(() -> {
      try {
        Parent root = anchor.getScene().getRoot();
        if (root instanceof StackPane sp) {
          ToastManager.init(sp);
        } else {
          Scene scene = anchor.getScene();
          StackPane overlay = new StackPane();
          overlay.getChildren().add(root);
          scene.setRoot(overlay);
          ToastManager.init(overlay);
        }
      } catch (Exception e) {
        System.err.println("[" + getClass().getSimpleName() + "] ToastManager init failed: "
            + e.getMessage());
      }
    });
  }


  protected final void registerPushListener(Consumer<String> handler) {
    removePushListener(); // đảm bảo không đăng ký 2 lần
    this.pushListener = handler;
    ServerConnection.getInstance().addPushListener(pushListener);
  }

  protected final void removePushListener() {
    if (pushListener != null) {
      ServerConnection.getInstance().removePushListener(pushListener);
      pushListener = null;
    }
  }

  protected void loadBalance(Label balanceLabel) {
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
            balanceLabel.setText(String.format("%,.0f VNĐ", v));
          } catch (NumberFormatException e) {
            balanceLabel.setText(amt + " VNĐ");
          }
        } else {
          balanceLabel.setText("---");
        }
      });
    }, getClass().getSimpleName() + "-balance-thread").start();
  }

  /** Variant dùng khi label cần prefix, ví dụ AdminDashboard: "Số dư: 1.000.000 VNĐ". */
  protected void loadBalanceWithPrefix(Label balanceLabel, String prefix) {
    if (balanceLabel == null) {
      return;
    }
    new Thread(() -> {
      String res = ServerConnection.getInstance().sendAndReceive(Protocol.CMD_GET_BALANCE);
      Platform.runLater(() -> {
        if (res != null && res.startsWith(Protocol.RES_BALANCE_INFO)) {
          String[] p = res.split("\\|", -1);
          try {
            double v = Double.parseDouble(p.length >= 2 ? p[1] : "0");
            balanceLabel.setText(prefix + String.format("%,.0f VNĐ", v));
          } catch (NumberFormatException e) {
            balanceLabel.setText(prefix + "---");
          }
        }
      });
    }, getClass().getSimpleName() + "-balance-thread").start();
  }

  /**
   * Cập nhật label số dư từ dữ liệu push (NOTI_BALANCE_CHANGED).
   * Tránh gọi server thêm 1 lần khi đã có giá trị mới trong push payload.
   */
  protected void updateBalanceLabelFromPush(Label balanceLabel, String rawValue) {
    if (balanceLabel == null || rawValue == null) {
      return;
    }
    try {
      double v = Double.parseDouble(rawValue);
      balanceLabel.setText(String.format("%,.0f VNĐ", v));
    } catch (NumberFormatException ignored) {
      // Không crash nếu server gửi format lạ
    }
  }
}