package tn.jobnest.gentretien.controller;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.jobnest.gentretien.model.Notification;

/**
 * Toast de notification non-bloquant — s'affiche en haut à droite
 * pendant 5 secondes avec une barre de progression, puis disparaît.
 */
public class NotificationToast {

    /**
     * Affiche le toast sur la scène donnée.
     * @param stage  fenêtre principale (pour positionner le popup)
     * @param notif  notification à afficher
     */
    public static void show(Stage stage, Notification notif) {
        Popup popup = new Popup();

        // ── Racine du toast ──────────────────────────────────────────
        VBox root = new VBox(0);
        root.setPrefWidth(340);
        root.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(15,30,55,0.22),20,0,0,6);");

        // ── Barre colorée en haut ────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setPrefHeight(4);
        topBar.setStyle("-fx-background-color:" + notif.getAccentColor()
                + "; -fx-background-radius:14 14 0 0;");

        // ── Corps ────────────────────────────────────────────────────
        HBox body = new HBox(12);
        body.setPadding(new Insets(14, 16, 14, 16));
        body.setAlignment(Pos.TOP_LEFT);

        // Icône
        Label icon = new Label(notif.getIcon());
        icon.setStyle("-fx-font-size:26px;");
        StackPane iconPane = new StackPane(icon);
        iconPane.setMinSize(46, 46);
        iconPane.setMaxSize(46, 46);
        iconPane.setStyle("-fx-background-color:" + lighten(notif.getAccentColor())
                + "; -fx-background-radius:23;");

        // Texte
        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titre = new Label(notif.getTitre());
        titre.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;"
                + "-fx-font-family:'Segoe UI';");

        Label msg = new Label(notif.getMessage());
        msg.setWrapText(true);
        msg.setMaxWidth(230);
        msg.setStyle("-fx-font-size:12px; -fx-text-fill:#475569; -fx-font-family:'Segoe UI';");

        textBox.getChildren().addAll(titre, msg);

        // Bouton fermer
        Label btnClose = new Label("✕");
        btnClose.setStyle("-fx-font-size:13px; -fx-text-fill:#94A3B8; -fx-cursor:hand;");
        btnClose.setOnMouseClicked(e -> popup.hide());

        body.getChildren().addAll(iconPane, textBox, btnClose);

        // ── Barre de progression (timer) ─────────────────────────────
        StackPane progressContainer = new StackPane();
        progressContainer.setPrefHeight(3);
        progressContainer.setMaxHeight(3);
        progressContainer.setStyle("-fx-background-color:#E2E8F0; -fx-background-radius:0 0 14 14;");

        Rectangle progressBar = new Rectangle();
        progressBar.setHeight(3);
        progressBar.setWidth(340);
        progressBar.setStyle("-fx-fill:" + notif.getAccentColor() + ";");
        progressBar.setArcWidth(6);
        progressBar.setArcHeight(6);
        StackPane.setAlignment(progressBar, Pos.CENTER_LEFT);
        progressContainer.getChildren().add(progressBar);

        root.getChildren().addAll(topBar, body, progressContainer);
        popup.getContent().add(root);

        // ── Position : haut droite ───────────────────────────────────
        double x = stage.getX() + stage.getWidth()  - 360;
        double y = stage.getY() + 60;
        popup.show(stage, x, y);

        // ── Animation d'entrée ───────────────────────────────────────
        root.setOpacity(0);
        root.setTranslateY(-10);
        FadeTransition fadeIn   = new FadeTransition(Duration.millis(250), root);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), root);
        slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();

        // ── Barre de progression animée (5 s) ────────────────────────
        final int DUREE_MS = 5000;
        Timeline progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressBar.widthProperty(), 340)),
                new KeyFrame(Duration.millis(DUREE_MS),
                        new KeyValue(progressBar.widthProperty(), 0))
        );

        // ── Auto-fermeture ───────────────────────────────────────────
        PauseTransition pause = new PauseTransition(Duration.millis(DUREE_MS));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setToValue(0);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), root);
            slideOut.setToY(-10);
            ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
            exit.setOnFinished(ev -> popup.hide());
            exit.play();
        });

        progressTimeline.play();
        pause.play();
    }

    private static String lighten(String hex) {
        switch (hex) {
            case "#F97316": return "#FFF7ED";
            case "#2563EB": return "#EFF6FF";
            case "#EF4444": return "#FEF2F2";
            default:        return "#F1F5F9";
        }
    }
}