package tn.jobnest.gentretien.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import tn.jobnest.gentretien.model.Notification;
import tn.jobnest.gentretien.service.NotificationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur du panneau latéral de notifications JobNest.
 * Géré par NotificationPanelController — inclus dans le layout principal.
 */
public class NotificationPanelController {

    @FXML private VBox    panelRoot;
    @FXML private VBox    notifListVBox;
    @FXML private Label   emptyLabel;
    @FXML private Button  btnToutLire;
    @FXML private Label   headerCountLabel;

    private final NotificationService notifService = NotificationService.getInstance();
    private static final int RECRUTEUR_ID = 1;

    @FXML
    public void initialize() {
        charger();
    }

    // ── Chargement ──────────────────────────────────────────────────
    public void charger() {
        notifListVBox.getChildren().clear();
        try {
            List<Notification> notifs = notifService.getAll(RECRUTEUR_ID);
            long nonLues = notifs.stream().filter(n -> !n.isLue()).count();
            headerCountLabel.setText(nonLues > 0 ? nonLues + " non lue(s)" : "À jour ✓");
            headerCountLabel.setStyle(nonLues > 0
                    ? "-fx-text-fill:#EF4444; -fx-font-weight:bold;"
                    : "-fx-text-fill:#16A34A; -fx-font-weight:bold;");

            if (notifs.isEmpty()) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                btnToutLire.setDisable(true);
            } else {
                emptyLabel.setVisible(false);
                emptyLabel.setManaged(false);
                btnToutLire.setDisable(nonLues == 0);
                for (Notification n : notifs) {
                    notifListVBox.getChildren().add(buildNotifCard(n));
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotifPanel] Erreur chargement : " + e.getMessage());
        }
    }

    // ── Marquer tout lu ──────────────────────────────────────────────
    @FXML
    private void marquerToutesLues() {
        try {
            notifService.marquerToutesLues(RECRUTEUR_ID);
            charger();
        } catch (SQLException e) {
            System.err.println("[NotifPanel] Erreur marquer lues : " + e.getMessage());
        }
    }

    // ── Construction d'une carte notification ───────────────────────
    private HBox buildNotifCard(Notification n) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(Double.MAX_VALUE);

        String baseStyle = "-fx-background-radius:12; -fx-border-radius:12; " +
                "-fx-border-width:1; -fx-cursor:hand;";
        if (!n.isLue()) {
            card.setStyle(baseStyle +
                    "-fx-background-color:#EFF6FF; -fx-border-color:#BFDBFE;");
        } else {
            card.setStyle(baseStyle +
                    "-fx-background-color:#FAFAFA; -fx-border-color:#E2E8F0;");
        }

        // Icône colorée
        Label iconLabel = new Label(n.getIcon());
        iconLabel.setStyle("-fx-font-size:22px;");
        StackPane iconPane = new StackPane(iconLabel);
        iconPane.setMinSize(44, 44);
        iconPane.setMaxSize(44, 44);
        iconPane.setStyle("-fx-background-color:" + lighten(n.getAccentColor())
                + "; -fx-background-radius:22;");

        // Contenu texte
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Titre + badge "Nouveau"
        HBox titreRow = new HBox(8);
        titreRow.setAlignment(Pos.CENTER_LEFT);
        Label titreLabel = new Label(n.getTitre());
        titreLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;");
        titreRow.getChildren().add(titreLabel);
        if (!n.isLue()) {
            Label badge = new Label("Nouveau");
            badge.setStyle("-fx-background-color:" + n.getAccentColor()
                    + "; -fx-text-fill:white; -fx-font-size:9px; -fx-font-weight:bold;"
                    + "-fx-background-radius:10; -fx-padding:2 7 2 7;");
            titreRow.getChildren().add(badge);
        }

        Label msgLabel = new Label(n.getMessage());
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");
        msgLabel.setMaxWidth(260);

        String dateStr = n.getDateCreation()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        Label dateLabel = new Label("🕐 " + dateStr);
        dateLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#94A3B8;");

        content.getChildren().addAll(titreRow, msgLabel, dateLabel);

        // Bouton marquer lue
        if (!n.isLue()) {
            Button btnLue = new Button("✓");
            btnLue.setTooltip(new Tooltip("Marquer comme lue"));
            btnLue.setStyle("-fx-background-color:transparent; -fx-text-fill:#2563EB;"
                    + "-fx-font-size:16px; -fx-font-weight:bold; -fx-cursor:hand;"
                    + "-fx-border-width:0; -fx-padding:0;");
            btnLue.setOnAction(ev -> {
                try {
                    notifService.marquerLue(n.getId());
                    charger();
                } catch (SQLException ex) {
                    System.err.println("[NotifPanel] Erreur marquer lue : " + ex.getMessage());
                }
            });
            card.getChildren().addAll(iconPane, content, btnLue);
        } else {
            card.getChildren().addAll(iconPane, content);
        }

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                + "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.13),10,0,0,3);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.13),10,0,0,3);", "")));

        return card;
    }

    /** Version allégée de la couleur pour fond de l'icône */
    private String lighten(String hexColor) {
        switch (hexColor) {
            case "#F97316": return "#FFF7ED";
            case "#2563EB": return "#EFF6FF";
            case "#EF4444": return "#FEF2F2";
            default:        return "#F1F5F9";
        }
    }

    // ── Animation d'entrée du panel ──────────────────────────────────
    public void playEntryAnimation() {
        panelRoot.setOpacity(0);
        panelRoot.setTranslateX(20);
        FadeTransition fade = new FadeTransition(Duration.millis(220), panelRoot);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), panelRoot);
        slide.setToX(0);
        new ParallelTransition(fade, slide).play();
    }
}