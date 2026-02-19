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
import tn.jobnest.gentretien.controller.GestionCandidaturesController;
import tn.jobnest.gentretien.service.CandidatureService;
import tn.jobnest.gentretien.model.CandidatureDTO;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Entretiencontroller {

    @FXML
    private VBox entretiensVBox;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> comboType;
    @FXML
    private ComboBox<String> comboStatut;
    @FXML
    private Label planifiesCount;
    @FXML
    private Label terminesCount;
    @FXML
    private Label semaineCount;

    private final Entretienservice service = new Entretienservice();
    private List<Entretien> allEntretiens;
    private final int currentRecruteurId = 1;

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("Tous les types", "présentiel", "visio"));
        comboType.setValue("Tous les types");
        comboStatut.setItems(FXCollections.observableArrayList("Tous les statuts", "proposé",  "réalisé", "annulé"));
        comboStatut.setValue("Tous les statuts");

        comboType.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        comboStatut.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, newVal) -> filterAndDisplay());

        rafraichirListe();
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/feedback-interface.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Feedbacks");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'interface des feedbacks : " + ex.getMessage());
        }
    }

    private void rafraichirListe() {
        try {
            List<Entretien> tous = service.afficher();
            allEntretiens = tous.stream()
                    .filter(e -> e.getIdRecruteur() == currentRecruteurId)
                    .collect(Collectors.toList());
            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les entretiens : " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allEntretiens == null) return;

        long planifies = allEntretiens.stream().filter(e -> "proposé".equals(e.getStatut()) ).count();
        long termines = allEntretiens.stream().filter(e -> "réalisé".equals(e.getStatut())).count();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        long cetteSemaine = allEntretiens.stream()
                .filter(e -> e.getDateEntretien() != null)
                .filter(e -> !LocalDate.from(e.getDateEntretien().toLocalDate()).isBefore(startOfWeek)
                        && !LocalDate.from(e.getDateEntretien().toLocalDate()).isAfter(endOfWeek))
                .count();

        planifiesCount.setText(String.valueOf(planifies));
        terminesCount.setText(String.valueOf(termines));
        semaineCount.setText(String.valueOf(cetteSemaine));
    }

    private void filterAndDisplay() {
        if (allEntretiens == null || allEntretiens.isEmpty()) {
            entretiensVBox.getChildren().clear();
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        String selType = comboType.getValue();
        String selStatut = comboStatut.getValue();

        entretiensVBox.getChildren().clear();

        for (Entretien e : allEntretiens) {
            try {
                String titreOffre = service.getOffreTitre(e.getIdOffre()).toLowerCase();
                List<String> participants = service.getParticipants(e.getIdEntretien());
                String participantsStr = String.join(" ", participants).toLowerCase();

                boolean matchSearch = search.isEmpty()
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);
                boolean matchType = "Tous les types".equals(selType) || selType.equals(e.getTypeEntretien());
                boolean matchStatut = "Tous les statuts".equals(selStatut) || selStatut.equals(e.getStatut());

                if (matchSearch && matchType && matchStatut) {
                    entretiensVBox.getChildren().add(createEntretienCard(e, participants, titreOffre));
                }
            } catch (SQLException ex) {
                System.err.println("Erreur chargement données entretien #" + e.getIdEntretien());
            }
        }
    }

    private Node createEntretienCard(Entretien e, List<String> participants, String titreOffre) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(130);

        // ---- Avatar ----
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
        avatarLabel.getStyleClass().add("avatar-text");
        avatarLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        StackPane avatarStack = new StackPane();
        avatarStack.getChildren().addAll(avatar, avatarLabel);

        VBox avatarBox = new VBox(avatarStack);
        avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
        avatarBox.setPrefWidth(70);

        // ---- Détails ----
        VBox details = new VBox(8);
        details.setPrefWidth(400);
        details.setPadding(new Insets(5, 0, 5, 0));

        Label offreLabel = new Label(titreOffre);
        offreLabel.getStyleClass().add("card-title");
        offreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label participantsLabel;
        if (participants.size() > 1) {
            participantsLabel = new Label("👥 " + String.join(", ", participants));
        } else {
            participantsLabel = new Label("👤 " + (participants.isEmpty() ? "Aucun candidat" : participants.get(0)));
        }
        participantsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: 500;");

        String dateStr = (e.getDateEntretien() != null)
                ? e.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
                : "Date non définie";
        String heureStr = (e.getHeureDebut() != null)
                ? e.getHeureDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        long duree = calculateDuration(e);
        String dureeTxt = (duree > 0) ? "Durée: " + duree + " min" : "Durée non définie";

        Label dateTimeLabel = new Label("📅 " + dateStr + " à " + heureStr + " (" + dureeTxt + ")");
        dateTimeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        Label lieuVisioLabel;
        if ("présentiel".equals(e.getTypeEntretien())) {
            String lieu = (e.getLieu() != null && !e.getLieu().isEmpty()) ? e.getLieu() : "Lieu non défini";
            lieuVisioLabel = new Label("📍 " + lieu);
        } else {
            String lien = (e.getLienVisio() != null && !e.getLienVisio().isEmpty())
                    ? e.getLienVisio() : "Lien non défini";
            if (lien.length() > 30) lien = lien.substring(0, 27) + "...";
            lieuVisioLabel = new Label("🔗 " + lien);
        }
        lieuVisioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #3498db; -fx-font-weight: 500;");

        // Badge statut coloré
        Label statutBadge = buildStatutBadge(e.getStatut());

        Label noteLabel = null;
        if (e.getNoteRecruteur() != null && !e.getNoteRecruteur().isEmpty()) {
            noteLabel = new Label("📝 Note: " + e.getNoteRecruteur());
            noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e67e22; -fx-font-style: italic; -fx-font-weight: 500;");
        }

        details.getChildren().addAll(offreLabel, participantsLabel, dateTimeLabel, lieuVisioLabel, statutBadge);
        if (noteLabel != null) {
            details.getChildren().add(noteLabel);
        }

        // ---- Actions ----
        VBox actionsContainer = new VBox(8);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionsContainer.setPrefWidth(420);
        actionsContainer.setPadding(new Insets(5, 0, 5, 0));

        // Rangée 1 : Modifier (ou Réorganiser si annulé, ou grisé si réalisé) + Ajouter Feedback
        HBox actionsRow1 = new HBox(10);
        actionsRow1.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        boolean estAnnule = "annulé".equals(e.getStatut());
        boolean estRealise2 = "réalisé".equals(e.getStatut());

        Button btnModifier;
        if (estAnnule) {
            // BOUTON RÉORGANISER — remplace Modifier pour les entretiens annulés
            btnModifier = new Button("🔄 Réorganiser");
            btnModifier.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #F97316, #EA580C);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: 800;" +
                            "-fx-font-size: 13px;" +
                            "-fx-background-radius: 10px;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.4), 8, 0, 0, 3);"
            );
            btnModifier.setPrefWidth(145);
            btnModifier.setPrefHeight(42);
            btnModifier.setOnAction(ev -> reorganiserEntretien(e));
            Tooltip tipReorg = new Tooltip(
                    "🔄 Réorganiser cet entretien annulé\n" +
                            "→ Choisissez une nouvelle date\n" +
                            "→ Le statut repassera à 'proposé' automatiquement"
            );
            tipReorg.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnModifier, tipReorg);
        } else if (estRealise2) {
            // BOUTON MODIFIER DÉSACTIVÉ — entretien réalisé, non modifiable
            btnModifier = new Button("Modifier");
            btnModifier.setPrefWidth(120);
            btnModifier.setPrefHeight(42);
            btnModifier.setDisable(true);
            btnModifier.setStyle(
                    "-fx-background-color: #E2E8F0;" +
                            "-fx-text-fill: #94A3B8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10px;"
            );
            Tooltip tipLock = new Tooltip("🔒 Non modifiable\nUn entretien réalisé ne peut plus être modifié.");
            tipLock.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnModifier, tipLock);
        } else {
            btnModifier = new Button("Modifier");
            btnModifier.getStyleClass().add("button-primary");
            btnModifier.setPrefWidth(120);
            btnModifier.setPrefHeight(42);
            btnModifier.setOnAction(ev -> openForm(true, e));
        }

        // BOUTON "Ajouter Feedback"
        // Actif uniquement si statut == "réalisé" ET aucun feedback n'existe encore
        Button btnFeedback = new Button("💬 Feedback");
        btnFeedback.setPrefWidth(130);
        btnFeedback.setPrefHeight(42);

        boolean estRealise = "réalisé".equals(e.getStatut());
        FeedbackService feedbackService = new FeedbackService();

        if (estRealise) {
            // Vérifier en BD si un feedback existe déjà pour cet entretien
            boolean feedbackDejaExistant = false;
            try {
                feedbackDejaExistant = feedbackService.feedbackExists(e.getIdEntretien());
            } catch (SQLException ex) {
                System.err.println("Erreur vérification feedback #" + e.getIdEntretien() + " : " + ex.getMessage());
            }

            if (feedbackDejaExistant) {
                // Feedback déjà créé — bouton grisé
                btnFeedback.setText("✅ Feedback fait");
                btnFeedback.setPrefWidth(140);
                btnFeedback.setDisable(true);
                btnFeedback.setStyle(
                        "-fx-background-color: #D1FAE5;" +
                                "-fx-text-fill: #059669;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 10px;"
                );
                Tooltip tipDeja = new Tooltip(
                        "✅ Feedback déjà enregistré\n" +
                                "Un seul feedback est autorisé par entretien."
                );
                tipDeja.setStyle("-fx-font-size: 12px;");
                Tooltip.install(btnFeedback, tipDeja);
            } else {
                // Pas encore de feedback — bouton actif
                btnFeedback.getStyleClass().add("button-success");
                btnFeedback.setOnAction(ev -> ouvrirFeedbackPourEntretien(e));
                Tooltip tip = new Tooltip("Ajouter un feedback pour cet entretien réalisé");
                Tooltip.install(btnFeedback, tip);
            }
        } else {
            // Bouton grisé avec tooltip explicatif
            btnFeedback.setDisable(true);
            btnFeedback.setStyle(
                    "-fx-background-color: #BDC3C7; " +
                            "-fx-text-fill: #7F8C8D; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 10px;"
            );
            String raison;
            switch (e.getStatut() == null ? "" : e.getStatut()) {
                case "proposé":
                    raison = "L'entretien est encore en attente de confirmation par le candidat.";
                    break;

                case "annulé":
                    raison = "L'entretien a été annulé, aucun feedback possible.";
                    break;
                default:
                    raison = "Le feedback n'est disponible qu'une fois l'entretien réalisé.";
            }
            Tooltip tip = new Tooltip("⚠️ Feedback indisponible\n" + raison +
                    "\n\nLe statut sera automatiquement mis à 'réalisé'\nlorsque le candidat rejoindra l'entretien.");
            tip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnFeedback, tip);
        }

        actionsRow1.getChildren().addAll(btnModifier, btnFeedback);

        // Rangée 2 : Bouton Map/Visio + Supprimer
        HBox actionsRow2 = new HBox(10);
        actionsRow2.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnActionSpecifique;
        if ("présentiel".equals(e.getTypeEntretien())) {
            btnActionSpecifique = new Button(" Consulter map");
            btnActionSpecifique.getStyleClass().add("button-map");
            btnActionSpecifique.setPrefWidth(150);
            btnActionSpecifique.setPrefHeight(42);
            btnActionSpecifique.setOnAction(ev -> consulterMap(e));
        } else {
            btnActionSpecifique = new Button("📹 Rejoindre");
            btnActionSpecifique.getStyleClass().add("button-visio");
            btnActionSpecifique.setPrefWidth(130);
            btnActionSpecifique.setPrefHeight(42);
            btnActionSpecifique.setOnAction(ev -> rejoindre(e));

            if (e.getLienVisio() == null || e.getLienVisio().isBlank()) {
                btnActionSpecifique.setDisable(true);
                btnActionSpecifique.setText("❌ Lien indisponible");
                btnActionSpecifique.setPrefWidth(160);
            } else if (e.getDateEntretien() != null) {
                LocalDate dateEntretien = e.getDateEntretien().toLocalDate();
                LocalDate aujourdhui = LocalDate.now();

                if (!dateEntretien.equals(aujourdhui)) {
                    btnActionSpecifique.setDisable(true);
                    if (dateEntretien.isAfter(aujourdhui)) {
                        btnActionSpecifique.setText("⏳ Pas encore");
                        btnActionSpecifique.setPrefWidth(140);
                    } else {
                        btnActionSpecifique.setText("⛔ Expiré");
                        btnActionSpecifique.setPrefWidth(130);
                    }
                }
            }
        }

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setPrefWidth(120);
        btnSupprimer.setPrefHeight(42);

        if (estRealise2) {
            // Désactivé — un entretien réalisé ne peut pas être supprimé
            btnSupprimer.setDisable(true);
            btnSupprimer.setStyle(
                    "-fx-background-color: #E2E8F0;" +
                            "-fx-text-fill: #94A3B8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10px;"
            );
            Tooltip tipLockDel = new Tooltip("🔒 Non supprimable\nUn entretien réalisé ne peut pas être supprimé.");
            tipLockDel.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnSupprimer, tipLockDel);
        } else {
            btnSupprimer.getStyleClass().add("button-danger");
            btnSupprimer.setOnAction(ev -> supprimerEntretien(e));
        }

        actionsRow2.getChildren().addAll(btnActionSpecifique, btnSupprimer);

        actionsContainer.getChildren().addAll(actionsRow1, actionsRow2);

        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatarBox, details, actionsContainer);

        return card;
    }

    /**
     * Construit un badge coloré selon le statut de l'entretien.
     */
    private Label buildStatutBadge(String statut) {
        String emoji;
        String color;
        String bgColor;

        switch (statut == null ? "" : statut) {
            case "proposé":
                emoji = "🕐";
                color = "#D97706";
                bgColor = "#FEF3C7";
                break;

            case "réalisé":
                emoji = "🏁";
                color = "#059669";
                bgColor = "#D1FAE5";
                break;
            case "annulé":
                emoji = "❌";
                color = "#DC2626";
                bgColor = "#FEE2E2";
                break;
            default:
                emoji = "❓";
                color = "#6B7280";
                bgColor = "#F3F4F6";
        }

        Label badge = new Label(emoji + " " + (statut != null ? statut : "inconnu"));
        badge.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 3 10 3 10;" +
                        "-fx-background-radius: 20px;"
        );
        return badge;
    }

    private long calculateDuration(Entretien e) {
        if (e.getHeureDebut() == null || e.getHeureFin() == null) return 0;
        return TimeUnit.MILLISECONDS.toMinutes(e.getHeureFin().getTime() - e.getHeureDebut().getTime());
    }

    private void openForm(boolean edition, Entretien ent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            EntretienFormController ctrl = loader.getController();
            if (edition && ent != null) {
                ctrl.setEntretien(ent);
                stage.setTitle("Modifier l'entretien");
            } else {
                stage.setTitle("Ajouter un entretien");
            }
            stage.showAndWait();
            rafraichirListe();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    /**
     * Ouvre le formulaire en mode RÉORGANISATION pour un entretien annulé.
     * - Date vidée (le recruteur doit choisir une nouvelle date)
     * - Lien visio vidé si visio (doit être regénéré pour la nouvelle date)
     * - À l'enregistrement, le statut repassera automatiquement à "proposé"
     */
    private void reorganiserEntretien(Entretien e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            EntretienFormController ctrl = loader.getController();
            ctrl.setEntretienPourReorganisation(e);
            stage.setTitle("🔄 Réorganiser l'entretien #" + e.getIdEntretien());
            stage.showAndWait();
            rafraichirListe();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de réorganisation : " + ex.getMessage());
        }
    }

    /**
     * Ouvre le formulaire de feedback uniquement si l'entretien est "réalisé".
     * Méthode déclenchée par le bouton "💬 Feedback" sur la carte.
     */
    private void ouvrirFeedbackPourEntretien(Entretien e) {
        // Double-check au cas où le statut aurait changé entre le chargement et le clic
        if (!"réalisé".equals(e.getStatut())) {
            showAlert(Alert.AlertType.WARNING,
                    "Action impossible",
                    "Le feedback ne peut être ajouté que pour un entretien réalisé.\n\n" +
                            "Statut actuel : " + e.getStatut() + "\n\n" +
                            "Le statut sera automatiquement mis à 'réalisé' lorsque le candidat rejoindra l'entretien.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/feedback-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));

            FeedbackFormController ctrl = loader.getController();
            ctrl.setEntretien(e);

            stage.setTitle("💬 Feedback — Entretien #" + e.getIdEntretien());
            stage.showAndWait();

            rafraichirListe();

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de feedback : " + ex.getMessage());
        }
    }

    private void rejoindre(Entretien e) {
        if (e.getDateEntretien() != null) {
            LocalDate dateEntretien = e.getDateEntretien().toLocalDate();
            LocalDate aujourdhui = LocalDate.now();

            if (!dateEntretien.equals(aujourdhui)) {
                if (dateEntretien.isAfter(aujourdhui)) {
                    showAlert(Alert.AlertType.WARNING, "Accès impossible",
                            "Cet entretien est prévu pour le " +
                                    dateEntretien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n" +
                                    "Vous ne pouvez rejoindre la visioconférence que le jour de l'entretien.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Accès impossible",
                            "Cet entretien était prévu pour le " +
                                    dateEntretien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n" +
                                    "La date est passée, vous ne pouvez plus rejoindre cette visioconférence.");
                }
                return;
            }
        }

        if ("visio".equals(e.getTypeEntretien()) && e.getLienVisio() != null && !e.getLienVisio().isBlank()) {
            try {
                Desktop.getDesktop().browse(new URI(e.getLienVisio()));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le lien : " + ex.getMessage());
            }
        }
    }

    private void consulterMap(Entretien e) {
        if ("présentiel".equals(e.getTypeEntretien()) && e.getLieu() != null && !e.getLieu().isBlank()) {
            try {
                Alert choix = new Alert(Alert.AlertType.CONFIRMATION);
                choix.setTitle("Consulter la carte");
                choix.setHeaderText("Ouvrir Google Maps ?");
                choix.setContentText("📍 Lieu : " + e.getLieu());

                ButtonType btnCarte = new ButtonType("🗺️ Voir sur la carte");
                ButtonType btnItineraire = new ButtonType("🧭 Calculer itinéraire");
                ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

                choix.getButtonTypes().setAll(btnCarte, btnItineraire, btnAnnuler);

                Optional<ButtonType> result = choix.showAndWait();

                if (result.isPresent()) {
                    if (result.get() == btnCarte) {
                        String searchUrl = "https://www.google.com/maps/search/?api=1&query="
                                + java.net.URLEncoder.encode(e.getLieu(), "UTF-8");
                        Desktop.getDesktop().browse(new URI(searchUrl));

                    } else if (result.get() == btnItineraire) {
                        TextInputDialog dialog = new TextInputDialog("Ma position");
                        dialog.setTitle("Point de départ");
                        dialog.setHeaderText("Calculer l'itinéraire");
                        dialog.setContentText("Votre adresse de départ :");

                        Optional<String> depart = dialog.showAndWait();
                        if (depart.isPresent() && !depart.get().trim().isEmpty()) {
                            String itineraireUrl = String.format(
                                    "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s&travelmode=driving",
                                    java.net.URLEncoder.encode(depart.get(), "UTF-8"),
                                    java.net.URLEncoder.encode(e.getLieu(), "UTF-8")
                            );
                            Desktop.getDesktop().browse(new URI(itineraireUrl));
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible d'ouvrir Google Maps : " + ex.getMessage());
            }
        }
    }

    private void supprimerEntretien(Entretien e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cet entretien ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.delete(e.getIdEntretien());
                rafraichirListe();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Entretien supprimé avec succès.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression : " + ex.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/GestionCandidatures.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Candidatures");
            stage.show();

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'interface des candidatures : " + ex.getMessage());
        }
    }
}