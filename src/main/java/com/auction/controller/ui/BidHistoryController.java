package com.auction.controller.ui;

import com.auction.network.client.ServerConnection;
import com.auction.network.protocol.Protocol;
import com.auction.util.core.BidHistoryManager;
import com.auction.util.core.BidHistoryManager.HistoryRecord;
import com.auction.util.core.BidHistoryManager.Result;
import com.auction.views.java.AuctionListView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BidHistoryController implements Initializable {

    @FXML private ListView<HistoryRecord> historyList;
    @FXML private Label totalLabel;
    @FXML private Label winLabel;
    @FXML private Label loseLabel;
    @FXML private Label rateLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button tabAll;
    @FXML private Button tabWin;
    @FXML private Button tabLose;

    private String username;
    private String activeTab = "all";

    private static final String TAB_ACTIVE =
            "-fx-background-color: #111827; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 20; " +
                    "-fx-padding: 6 18; -fx-cursor: hand; -fx-font-size: 12px; " +
                    "-fx-border-color: transparent;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #6B7280; " +
                    "-fx-background-radius: 20; -fx-padding: 6 18; " +
                    "-fx-cursor: hand; -fx-font-size: 12px; " +
                    "-fx-border-color: #E5E7EB; -fx-border-radius: 20; -fx-border-width: 1;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        historyList.setCellFactory(lv -> new HistoryCell());
    }

    public void setUsername(String u) {
        this.username = u;
        loadFromServer();
    }

    private void loadFromServer() {
        new Thread(() -> {
            try {
                String res = ServerConnection.getInstance()
                        .sendAndReceive(Protocol.CMD_GET_BID_HISTORY);

                if (res != null && res.startsWith(Protocol.RES_BID_HISTORY)) {
                    String json = res.substring(
                            Protocol.RES_BID_HISTORY.length() + Protocol.SEPARATOR.length());

                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    BidHistoryManager mgr = BidHistoryManager.getInstance();

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        String auctionId  = obj.has("auctionId")  ? obj.get("auctionId").getAsString()  : "—";
                        String itemName   = obj.has("itemName")   ? obj.get("itemName").getAsString()   : "—";
                        String itemType   = obj.has("itemType")   ? obj.get("itemType").getAsString()   : "";
                        double finalPrice = obj.has("finalPrice") ? obj.get("finalPrice").getAsDouble() : 0;
                        String result     = obj.has("result")     ? obj.get("result").getAsString()     : "LOSE";

                        String priceStr = String.format("%,.0f VNĐ", finalPrice);
                        Result r = "WIN".equalsIgnoreCase(result) ? Result.WIN : Result.LOSE;

                        mgr.addRecord(auctionId, itemName, itemType, priceStr, r);
                    }
                }
            } catch (Exception e) {
                System.err.println("[BidHistoryController] Lỗi load: " + e.getMessage());
            }

            Platform.runLater(() -> {
                updateStats();
                applyTab(activeTab);
            });
        }, "bid-history-load").start();
    }

    @FXML private void handleTabAll()  { applyTab("all");  }
    @FXML private void handleTabWin()  { applyTab("win");  }
    @FXML private void handleTabLose() { applyTab("lose"); }

    private void applyTab(String tab) {
        this.activeTab = tab;
        tabAll.setStyle(TAB_INACTIVE);
        tabWin.setStyle(TAB_INACTIVE);
        tabLose.setStyle(TAB_INACTIVE);

        List<HistoryRecord> source = switch (tab) {
            case "win"  -> { tabWin.setStyle(TAB_ACTIVE);  yield BidHistoryManager.getInstance().getWins();  }
            case "lose" -> { tabLose.setStyle(TAB_ACTIVE); yield BidHistoryManager.getInstance().getLoses(); }
            default     -> { tabAll.setStyle(TAB_ACTIVE);  yield BidHistoryManager.getInstance().getAll();   }
        };
        historyList.setItems(FXCollections.observableArrayList(source));
    }

    private void updateStats() {
        BidHistoryManager m = BidHistoryManager.getInstance();
        if (totalLabel    != null) totalLabel.setText(String.valueOf(m.totalCount()));
        if (winLabel      != null) winLabel.setText(String.valueOf(m.winCount()));
        if (loseLabel     != null) loseLabel.setText(String.valueOf(m.loseCount()));
        if (rateLabel     != null) rateLabel.setText(m.winRate());
        if (subtitleLabel != null) subtitleLabel.setText(m.totalCount() + " phiên đã tham gia");
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) historyList.getScene().getWindow();
        new AuctionListView(stage, username).show();
    }

    private class HistoryCell extends ListCell<HistoryRecord> {
        private final HBox      card       = new HBox(14);
        private final StackPane iconWrap   = new StackPane();
        private final Label     iconLabel  = new Label();
        private final VBox      content    = new VBox(3);
        private final HBox      titleRow   = new HBox(8);
        private final Label     itemName   = new Label();
        private final Label     badge      = new Label();
        private final Label     detail     = new Label();
        private final HBox      bottomRow  = new HBox(12);
        private final Label     dateLabel  = new Label();
        private final Label     priceLabel = new Label();

        HistoryCell() {
            iconWrap.setPrefSize(44, 44);
            iconWrap.setMinSize(44, 44);
            iconWrap.setMaxSize(44, 44);
            iconWrap.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-font-size: 20px;");
            iconWrap.getChildren().add(iconLabel);

            itemName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            itemName.setMaxWidth(280);
            badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 2 8;");
            detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

            titleRow.setAlignment(Pos.CENTER_LEFT);
            titleRow.getChildren().addAll(itemName, badge);

            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.getChildren().addAll(dateLabel, priceLabel);

            content.getChildren().addAll(titleRow, detail, bottomRow);
            HBox.setHgrow(content, Priority.ALWAYS);

            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 14, 14, 16));
            card.getChildren().addAll(iconWrap, content);

            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            setText(null);
        }

        @Override
        protected void updateItem(HistoryRecord rec, boolean empty) {
            super.updateItem(rec, empty);
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            if (empty || rec == null) { setGraphic(null); return; }

            boolean win  = rec.isWin();
            String  type = rec.getItemType();

            String iconTxt, iconBg;
            if      ("Art".equals(type))         { iconTxt = "🎨"; iconBg = "#FEE2E2"; }
            else if ("Electronics".equals(type)) { iconTxt = "💻"; iconBg = "#DBEAFE"; }
            else if ("Vehicle".equals(type))     { iconTxt = "🚗"; iconBg = "#D1FAE5"; }
            else                                 { iconTxt = "📦"; iconBg = "#F3F4F6"; }

            iconLabel.setText(iconTxt);
            iconWrap.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 22;");

            itemName.setText(rec.getItemName());
            detail.setText("ID: " + rec.getAuctionId());
            dateLabel.setText(rec.getDate() != null ? rec.getDate() : "");
            priceLabel.setText(rec.getFinalPrice());

            if (win) {
                badge.setText("Thắng");
                badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 12; -fx-padding: 2 8;" +
                        "-fx-background-color: #D1FAE5; -fx-text-fill: #15803D;");
                priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #15803D;");
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                        "-fx-border-color: transparent transparent transparent #22C55E;" +
                        "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 12 12 0;");
            } else {
                badge.setText("Thua");
                badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 12; -fx-padding: 2 8;" +
                        "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;");
                priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                        "-fx-border-color: transparent transparent transparent #EF4444;" +
                        "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 12 12 0;");
            }

            VBox outer = new VBox(card);
            outer.setPadding(new Insets(0, 0, 8, 0));
            setGraphic(outer);
            setText(null);
        }
    }
}