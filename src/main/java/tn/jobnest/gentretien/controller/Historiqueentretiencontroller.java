package tn.jobnest.gentretien.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.service.Entretienservice;
import tn.jobnest.gentretien.service.FeedbackService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Historiqueentretiencontroller {

    @FXML private VBox historiqueVBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> comboType;
    @FXML private ComboBox<String> comboStatutHistorique;
    @FXML private ComboBox<String> comboFeedback;
    @FXML private Label totalRealises;
    @FXML private Label avecFeedbackCount;
    @FXML private Label sansFeedbackCount;
    @FXML private Label totalAnnules;

    private final Entretienservice service = new Entretienservice();
    private final FeedbackService feedbackService = new FeedbackService();
    private List<Entretien> allHistorique;
    private final int currentRecruteurId = 1;

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList(
                "Tous les types", "présentiel", "visio"));
        comboType.setValue("Tous les types");

        comboStatutHistorique.setItems(FXCollections.observableArrayList(
                "Tous (réalisés + annulés)", "🏁 Réalisés uniquement", "❌ Annulés uniquement"));
        comboStatutHistorique.setValue("Tous (réalisés + annulés)");

        comboFeedback.setItems(FXCollections.observableArrayList(
                "Tous les feedbacks", "Avec feedback", "Sans feedback"));
        comboFeedback.setValue("Tous les feedbacks");

        comboType.valueProperty().addListener((obs, old, nv) -> filterAndDisplay());
        comboStatutHistorique.valueProperty().addListener((obs, old, nv) -> filterAndDisplay());
        comboFeedback.valueProperty().addListener((obs, old, nv) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, nv) -> filterAndDisplay());

        chargerHistorique();
    }

    @FXML
    private void actualiserHistorique(ActionEvent event) {
        chargerHistorique();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation", "L'historique a été actualisé.");
    }

    private void chargerHistorique() {
        try {
            int nbExpires = service.annulerEntretiensExpires();
            if (nbExpires > 0)
                System.out.println("[Auto-annulation] " + nbExpires + " entretien(s) → annulé(s)");

            List<Entretien> tous = service.afficher();
            allHistorique = tous.stream()
                    .filter(e -> e.getIdRecruteur() == currentRecruteurId)
                    .filter(e -> "réalisé".equals(e.getStatut()) || "annulé".equals(e.getStatut()))
                    .collect(Collectors.toList());

            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible de charger l'historique : " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allHistorique == null) return;
        long totalR = allHistorique.stream().filter(e -> "réalisé".equals(e.getStatut())).count();
        long totalA = allHistorique.stream().filter(e -> "annulé".equals(e.getStatut())).count();
        long avecFb = allHistorique.stream()
                .filter(e -> "réalisé".equals(e.getStatut()))
                .filter(e -> {
                    try { return feedbackService.feedbackExists(e.getIdEntretien()); }
                    catch (SQLException ex) { return false; }
                }).count();
        long sansFb = totalR - avecFb;
        totalRealises.setText(String.valueOf(totalR));
        totalAnnules.setText(String.valueOf(totalA));
        avecFeedbackCount.setText(String.valueOf(avecFb));
        sansFeedbackCount.setText(String.valueOf(sansFb));
    }

    private void filterAndDisplay() {
        historiqueVBox.getChildren().clear();
        if (allHistorique == null || allHistorique.isEmpty()) {
            afficherMessageVide("Aucun entretien dans l'historique pour le moment.");
            return;
        }
        String search      = searchField.getText().trim().toLowerCase();
        String selType     = comboType.getValue();
        String selStatut   = comboStatutHistorique.getValue();
        String selFeedback = comboFeedback.getValue();
        boolean auMoinsUn  = false;

        for (Entretien e : allHistorique) {
            try {
                String titreOffre         = service.getOffreTitre(e.getIdOffre()).toLowerCase();
                List<String> participants = service.getParticipants(e.getIdEntretien());
                String participantsStr    = String.join(" ", participants).toLowerCase();

                boolean estRealise  = "réalisé".equals(e.getStatut());
                boolean estAnnule   = "annulé".equals(e.getStatut());
                boolean hasFeedback = estRealise && feedbackService.feedbackExists(e.getIdEntretien());

                boolean matchSearch = search.isEmpty()
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);
                boolean matchType = "Tous les types".equals(selType)
                        || selType.equals(e.getTypeEntretien());

                boolean matchStatut;
                if ("🏁 Réalisés uniquement".equals(selStatut))    matchStatut = estRealise;
                else if ("❌ Annulés uniquement".equals(selStatut)) matchStatut = estAnnule;
                else                                                 matchStatut = true;

                boolean matchFeedback;
                if ("Avec feedback".equals(selFeedback))
                    matchFeedback = estRealise && hasFeedback;
                else if ("Sans feedback".equals(selFeedback))
                    matchFeedback = (estRealise && !hasFeedback) || estAnnule;
                else
                    matchFeedback = true;

                if (matchSearch && matchType && matchStatut && matchFeedback) {
                    historiqueVBox.getChildren().add(
                            createHistoriqueCard(e, participants, titreOffre, hasFeedback));
                    auMoinsUn = true;
                }
            } catch (SQLException ex) {
                System.err.println("Erreur chargement historique entretien #" + e.getIdEntretien());
            }
        }
        if (!auMoinsUn) afficherMessageVide("Aucun résultat pour ces filtres.");
    }

    private void afficherMessageVide(String message) {
        Label empty = new Label(message);
        empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-padding: 30;");
        historiqueVBox.getChildren().add(empty);
    }

    private Node createHistoriqueCard(Entretien e, List<String> participants,
                                      String titreOffre, boolean hasFeedback) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(130);

        boolean estAnnule  = "annulé".equals(e.getStatut());
        boolean estRealise = "réalisé".equals(e.getStatut());

        String couleurBordure = estAnnule ? "#DC2626" : (hasFeedback ? "#059669" : "#F97316");
        card.setStyle(
                "-fx-border-color: " + couleurBordure + ";" +
                        "-fx-border-width: 0 0 0 5;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);");

        String participantName = participants.isEmpty() ? "Candidat" : participants.get(0);
        String initials = participantName.chars()
                .filter(Character::isUpperCase)
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
        if (initials.isEmpty() && !participants.isEmpty()) {
            String[] parts = participantName.split(" ");
            initials = parts[0].substring(0, 1);
            if (parts.length > 1) initials += parts[1].substring(0, 1);
        } else if (initials.isEmpty()) {
            initials = "C";
        }
        Circle avatar = new Circle(25);
        avatar.getStyleClass().add("avatar");
        Label avatarLabel = new Label(initials);
        avatarLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane avatarStack = new StackPane();
        avatarStack.getChildren().addAll(avatar, avatarLabel);
        VBox avatarBox = new VBox(avatarStack);
        avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
        avatarBox.setPrefWidth(70);

        VBox details = new VBox(7);
        details.setPrefWidth(430);
        details.setPadding(new Insets(5, 0, 5, 0));

        Label offreLabel = new Label(titreOffre);
        offreLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1E3A5F;");

        String participantsText = participants.size() > 1
                ? "👥 " + String.join(", ", participants)
                : "👤 " + (participants.isEmpty() ? "Aucun candidat" : participants.get(0));
        Label participantsLabel = new Label(participantsText);
        participantsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-font-weight: 500;");

        String dateStr = (e.getDateEntretien() != null)
                ? e.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
                : "Date non définie";
        String heureStr = (e.getHeureDebut() != null)
                ? e.getHeureDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
        long duree = (e.getHeureDebut() != null && e.getHeureFin() != null)
                ? TimeUnit.MILLISECONDS.toMinutes(
                e.getHeureFin().getTime() - e.getHeureDebut().getTime()) : 0;
        Label dateTimeLabel = new Label("📅 " + dateStr + " à " + heureStr
                + (duree > 0 ? " (" + duree + " min)" : ""));
        dateTimeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label lieuLabel;
        if ("présentiel".equals(e.getTypeEntretien())) {
            String lieu = (e.getLieu() != null && !e.getLieu().isEmpty()) ? e.getLieu() : "Lieu non défini";
            lieuLabel = new Label("📍 " + lieu);
        } else {
            lieuLabel = new Label("🔗 Entretien en visioconférence");
        }
        lieuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3498db; -fx-font-weight: 500;");

        Label statutBadge = buildStatutHistoriqueBadge(e.getStatut(), hasFeedback);
        details.getChildren().addAll(offreLabel, participantsLabel, dateTimeLabel, lieuLabel, statutBadge);

        if (e.getNoteRecruteur() != null && !e.getNoteRecruteur().isEmpty()) {
            Label noteLabel = new Label("📝 " + e.getNoteRecruteur());
            noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22; -fx-font-style: italic;");
            details.getChildren().add(noteLabel);
        }

        VBox actionsContainer = new VBox(10);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionsContainer.setPrefWidth(230);
        actionsContainer.setPadding(new Insets(5, 0, 5, 0));

        Label typeBadge = new Label("présentiel".equals(e.getTypeEntretien()) ? "🏢 Présentiel" : "💻 Visio");
        typeBadge.setStyle(
                "-fx-background-color: " + ("présentiel".equals(e.getTypeEntretien()) ? "#EDE9FE" : "#DBEAFE") + ";" +
                        "-fx-text-fill: "        + ("présentiel".equals(e.getTypeEntretien()) ? "#7C3AED" : "#1D4ED8") + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 20px;");

        Button btnAction;
        if (estAnnule) {
            btnAction = new Button("🔄 Réorganiser");
            btnAction.setPrefWidth(165);
            btnAction.setPrefHeight(42);
            btnAction.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #ffffff, #EA580C);" +
                            "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13px;" +
                            "-fx-background-radius: 10px; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgb(37,99,235), 8, 0, 0, 3);");
            btnAction.setOnAction(ev -> reorganiserEntretien(e));
            Tooltip tip = new Tooltip(
                    "🔄 Réorganiser cet entretien annulé\n" +
                            "→ Choisissez une nouvelle date\n" +
                            "→ Le statut repassera à 'proposé'");
            tip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnAction, tip);
        } else {
            if (hasFeedback) {
                btnAction = new Button("✅ Feedback enregistré");
                btnAction.setPrefWidth(185);
                btnAction.setPrefHeight(42);
                btnAction.setDisable(true);
                btnAction.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #059669;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;");
                Tooltip tip = new Tooltip("✅ Feedback déjà enregistré\nUn seul feedback par entretien.");
                tip.setStyle("-fx-font-size: 12px;");
                Tooltip.install(btnAction, tip);
            } else {
                btnAction = new Button("💬 Ajouter Feedback");
                btnAction.setPrefWidth(175);
                btnAction.setPrefHeight(42);
                btnAction.setStyle(
                        "-fx-background-color:#1e3a8a; -fx-text-fill:#ffffff;" +
                                " -fx-text-fill: white;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 12px;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.35), 10, 0, 0, 3);");
                btnAction.setOnAction(ev -> ouvrirFeedbackPourEntretien(e));
                Tooltip.install(btnAction, new Tooltip("Ajouter un feedback pour cet entretien réalisé"));
            }
        }

        Button btnSupprimer = new Button(" Supprimer");
        btnSupprimer.setPrefWidth(165);
        btnSupprimer.setPrefHeight(42);

        if (estAnnule) {
            btnSupprimer.setStyle(
                    "-fx-background-color:#1e3a8a; -fx-text-fill:#ffffff;" +
                            " -fx-text-fill: white;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 12px;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.35), 10, 0, 0, 3);");
            btnSupprimer.setOnAction(ev -> supprimerEntretienHistorique(e));
            Tooltip.install(btnSupprimer,
                    new Tooltip("🗑️ Supprimer définitivement cet entretien annulé"));
        } else {
            btnSupprimer.setVisible(false);
            btnSupprimer.setManaged(false);
        }

        actionsContainer.getChildren().addAll(typeBadge, btnAction, btnSupprimer);

        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatarBox, details, actionsContainer);
        return card;
    }

    private Label buildStatutHistoriqueBadge(String statut, boolean hasFeedback) {
        String text, bgColor, textColor;
        if ("annulé".equals(statut)) {
            text = "❌ Annulé"; bgColor = "#FEE2E2"; textColor = "#DC2626";
        } else if (hasFeedback) {
            text = "🏁 Réalisé  •  ✅ Feedback enregistré";
            bgColor = "#D1FAE5"; textColor = "#059669";
        } else {
            text = "🏁 Réalisé  •  ⏳ En attente de feedback";
            bgColor = "#FEF3C7"; textColor = "#D97706";
        }
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 20px;");
        return badge;
    }

    private void supprimerEntretienHistorique(Entretien e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer cet entretien annulé ?");
        confirm.setContentText("Cette action est irréversible.\nL'entretien sera définitivement supprimé.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.delete(e.getIdEntretien());
                chargerHistorique();
                showAlert(Alert.AlertType.INFORMATION, "Suppression réussie",
                        "L'entretien annulé a été supprimé définitivement.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Échec de la suppression : " + ex.getMessage());
            }
        }
    }

    private void reorganiserEntretien(Entretien e) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            EntretienFormController ctrl = loader.getController();
            ctrl.setEntretienPourReorganisation(e);
            stage.setTitle("🔄 Réorganiser l'entretien #" + e.getIdEntretien());
            stage.showAndWait();
            chargerHistorique();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de réorganisation : " + ex.getMessage());
        }
    }

    private void ouvrirFeedbackPourEntretien(Entretien e) {
        if (!"réalisé".equals(e.getStatut())) {
            showAlert(Alert.AlertType.WARNING, "Action impossible",
                    "Le feedback n'est disponible que pour les entretiens réalisés.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/feedback-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            FeedbackFormController ctrl = loader.getController();
            ctrl.setEntretien(e);
            stage.setTitle("💬 Feedback — Entretien #" + e.getIdEntretien());
            stage.showAndWait();
            chargerHistorique();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de feedback : " + ex.getMessage());
        }
    }

    // =====================================================================
    // NAVIGATION SIDEBAR
    // =====================================================================
    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/entretien-view.fxml",
                "JobNest - Gestion des Entretiens", event);
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/feedback-interface.fxml",
                "JobNest - Gestion des Feedbacks", event);
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/GestionCandidatures.fxml",
                "JobNest - Gestion des Candidatures", event);
    }

    @FXML
    private void ouvrirProfil(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/profil-recruteur.fxml", "JobNest - Mon Profil", event);
    }

    // ────────────────────────────────────────────────────────────────
    //  ✅ NAVIGATION VERS OFFRES D'EMPLOI (SIDEBAR)
    // ────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirOffresEmploi(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/offre-emploi_view.fxml",
                "JobNest - Offres d'Emploi", event);
    }

    private void naviguerVers(String fxmlPath, String titre, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}