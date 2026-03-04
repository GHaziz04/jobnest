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

    @FXML private VBox             entretiensVBox;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> comboType;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Label            planifiesCount;
    @FXML private Label            terminesCount;
    @FXML private Label            semaineCount;

    private final Entretienservice service = new Entretienservice();
    private List<Entretien> allEntretiens;
    private List<Entretien> entretiensFiltres;
    private final int currentRecruteurId = 1;

    // ────────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("Tous les types", "présentiel", "visio"));
        comboType.setValue("Tous les types");

        comboStatut.setItems(FXCollections.observableArrayList("Tous les statuts", "proposé"));
        comboStatut.setValue("Tous les statuts");

        comboType  .valueProperty().addListener((obs, o, n) -> filterAndDisplay());
        comboStatut.valueProperty().addListener((obs, o, n) -> filterAndDisplay());
        searchField.textProperty() .addListener((obs, o, n) -> filterAndDisplay());

        rafraichirListe();
    }

    @FXML
    private void actualiserListe(ActionEvent event) {
        rafraichirListe();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation", "La liste des entretiens a été actualisée.");
    }

    // ────────────────────────────────────────────────────────────────
    //  ✅ NAVIGATION VERS OFFRES D'EMPLOI (SIDEBAR)
    // ────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirOffresEmploi(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre-emploi_view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Offres d'Emploi");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir les offres : " + ex.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  NAVIGATION SIDEBAR (existant)
    // ────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/feedback-interface.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Feedbacks");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir les feedbacks : " + ex.getMessage());
        }
    }

    @FXML
    private void ouvrirHistorique(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/historique-entretien.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL cssHist = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (cssHist != null) scene.getStylesheets().add(cssHist.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Historique des Entretiens");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir l'historique : " + ex.getMessage());
        }
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/GestionCandidatures.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL cssCand = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (cssCand != null) scene.getStylesheets().add(cssCand.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Candidatures");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir les candidatures : " + ex.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  CHARGEMENT DONNÉES
    // ────────────────────────────────────────────────────────────────
    private void rafraichirListe() {
        try {
            int nbExpires = service.annulerEntretiensExpires();
            if (nbExpires > 0)
                System.out.println("[Auto-annulation] " + nbExpires + " entretien(s) → annulé(s)");

            List<Entretien> tous = service.afficher();
            allEntretiens = tous.stream()
                    .filter(e -> e.getIdRecruteur() == currentRecruteurId)
                    .collect(Collectors.toList());

            entretiensFiltres = allEntretiens.stream()
                    .filter(e -> "proposé".equals(e.getStatut()))
                    .collect(Collectors.toList());

            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible de charger les entretiens : " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  STATISTIQUES
    // ────────────────────────────────────────────────────────────────
    private void updateStats() {
        if (allEntretiens == null) return;
        long planifies = allEntretiens.stream()
                .filter(e -> "proposé".equals(e.getStatut())).count();
        long termines = allEntretiens.stream()
                .filter(e -> "réalisé".equals(e.getStatut())).count();
        LocalDate today       = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek   = startOfWeek.plusDays(6);
        long cetteSemaine = allEntretiens.stream()
                .filter(e -> e.getDateEntretien() != null)
                .filter(e -> {
                    LocalDate d = e.getDateEntretien().toLocalDate();
                    return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                }).count();
        planifiesCount.setText(String.valueOf(planifies));
        terminesCount .setText(String.valueOf(termines));
        semaineCount  .setText(String.valueOf(cetteSemaine));
    }

    // ────────────────────────────────────────────────────────────────
    //  FILTRE + AFFICHAGE
    // ────────────────────────────────────────────────────────────────
    private void filterAndDisplay() {
        entretiensVBox.getChildren().clear();
        if (entretiensFiltres == null || entretiensFiltres.isEmpty()) {
            entretiensVBox.getChildren().add(emptyLabel("Aucun entretien en cours (proposé)."));
            return;
        }
        String search    = searchField.getText().trim().toLowerCase();
        String selType   = comboType.getValue();
        String selStatut = comboStatut.getValue();
        boolean auMoinsUn = false;

        for (Entretien e : entretiensFiltres) {
            try {
                String       titreOffre   = service.getOffreTitre(e.getIdOffre()).toLowerCase();
                List<String> participants = service.getParticipants(e.getIdEntretien());
                String participantsStr   = String.join(" ", participants).toLowerCase();

                boolean matchSearch = search.isEmpty()
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);
                boolean matchType   = "Tous les types".equals(selType)    || selType.equals(e.getTypeEntretien());
                boolean matchStatut = "Tous les statuts".equals(selStatut) || selStatut.equals(e.getStatut());

                if (matchSearch && matchType && matchStatut) {
                    entretiensVBox.getChildren().add(
                            createEntretienCard(e, participants, service.getOffreTitre(e.getIdOffre())));
                    auMoinsUn = true;
                }
            } catch (SQLException ex) {
                System.err.println("Erreur chargement entretien #" + e.getIdEntretien());
            }
        }
        if (!auMoinsUn)
            entretiensVBox.getChildren().add(emptyLabel("Aucun résultat pour ces filtres."));
    }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-padding: 30;");
        return l;
    }

    // ────────────────────────────────────────────────────────────────
    //  CARTE ENTRETIEN
    // ────────────────────────────────────────────────────────────────
    private Node createEntretienCard(Entretien e, List<String> participants, String titreOffre) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(130);

        String participantName = participants.isEmpty() ? "Candidat" : participants.get(0);
        String initials = participants.isEmpty() ? "C" : buildInitials(participantName);

        Circle avatar = new Circle(25);
        avatar.getStyleClass().add("avatar");
        Label avatarLabel = new Label(initials);
        avatarLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane avatarStack = new StackPane(avatar, avatarLabel);
        VBox avatarBox = new VBox(avatarStack);
        avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
        avatarBox.setPrefWidth(70);

        VBox details = new VBox(8);
        details.setPrefWidth(400);
        details.setPadding(new Insets(5, 0, 5, 0));

        Label offreLabel = new Label(titreOffre);
        offreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label participantsLabel = participants.size() > 1
                ? new Label("👥 " + String.join(", ", participants))
                : new Label("👤 " + (participants.isEmpty() ? "Aucun candidat" : participants.get(0)));
        participantsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: 500;");

        String dateStr = e.getDateEntretien() != null
                ? e.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
                : "Date non définie";
        String heureStr = e.getHeureDebut() != null
                ? e.getHeureDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
        long duree = calculateDuration(e);
        Label dateTimeLabel = new Label("📅 " + dateStr + " à " + heureStr
                + (duree > 0 ? "  (Durée : " + duree + " min)" : "  (Durée non définie)"));
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
        lieuVisioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e3a8a; -fx-font-weight: 500;");

        Label statutBadge = buildStatutBadge(e.getStatut());
        details.getChildren().addAll(offreLabel, participantsLabel, dateTimeLabel, lieuVisioLabel, statutBadge);

        if (e.getNoteRecruteur() != null && !e.getNoteRecruteur().isEmpty()) {
            Label noteLabel = new Label("📝 Note : " + e.getNoteRecruteur());
            noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e3a8a; -fx-font-style: italic;");
            details.getChildren().add(noteLabel);
        }

        VBox actionsContainer = new VBox(8);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionsContainer.setPrefWidth(320);
        actionsContainer.setPadding(new Insets(5, 0, 5, 0));

        boolean estAujourdhui = e.getDateEntretien() != null
                && e.getDateEntretien().toLocalDate().equals(LocalDate.now());

        HBox actionsRow1 = new HBox(10);
        actionsRow1.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("button-primary");
        btnModifier.setPrefWidth(120);
        btnModifier.setPrefHeight(42);

        if (estAujourdhui) {
            btnModifier.setDisable(true);
            btnModifier.setStyle(
                    "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8;" +
                            "-fx-font-weight: bold; -fx-background-radius: 10px;");
            Tooltip.install(btnModifier, new Tooltip(
                    "⚠️ Modification impossible\nL'entretien est prévu aujourd'hui."));
        } else {
            btnModifier.setOnAction(ev -> openForm(true, e));
        }
        actionsRow1.getChildren().add(btnModifier);

        HBox actionsRow2 = new HBox(10);
        actionsRow2.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnActionSpecifique;
        if ("présentiel".equals(e.getTypeEntretien())) {
            btnActionSpecifique = new Button("Consulter map");
            btnActionSpecifique.getStyleClass().add("button-map");
            btnActionSpecifique.setPrefWidth(155);
            btnActionSpecifique.setPrefHeight(42);
            if (estAujourdhui) {
                btnActionSpecifique.setOnAction(ev -> consulterMap(e));
            } else {
                btnActionSpecifique.setDisable(true);
                btnActionSpecifique.setStyle(
                        "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8;" +
                                "-fx-font-weight: bold; -fx-background-radius: 10px;");
            }
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
                LocalDate dateE = e.getDateEntretien().toLocalDate();
                LocalDate auj   = LocalDate.now();
                if (!dateE.equals(auj)) {
                    btnActionSpecifique.setDisable(true);
                    btnActionSpecifique.setText(dateE.isAfter(auj) ? "⏳ Pas encore" : "⛔ Expiré");
                    btnActionSpecifique.setPrefWidth(140);
                }
            }
        }

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setPrefWidth(120);
        btnSupprimer.setPrefHeight(42);

        if ("proposé".equals(e.getStatut()) && !estAujourdhui) {
            btnSupprimer.getStyleClass().add("button-danger");
            btnSupprimer.setOnAction(ev -> supprimerEntretien(e));
        } else if ("proposé".equals(e.getStatut()) && estAujourdhui) {
            btnSupprimer.setDisable(true);
            btnSupprimer.setStyle(
                    "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8;" +
                            "-fx-font-weight: bold; -fx-background-radius: 10px;");
        } else {
            btnSupprimer.setVisible(false);
            btnSupprimer.setManaged(false);
        }

        actionsRow2.getChildren().addAll(btnActionSpecifique, btnSupprimer);
        actionsContainer.getChildren().addAll(actionsRow1, actionsRow2);

        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatarBox, details, actionsContainer);
        return card;
    }

    // ────────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────────
    private String buildInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "?";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }

    private Label buildStatutBadge(String statut) {
        String emoji, color, bgColor;
        switch (statut == null ? "" : statut) {
            case "proposé":  emoji = "🕐"; color = "#D97706"; bgColor = "#FEF3C7"; break;
            case "confirmé": emoji = "✅"; color = "#1D4ED8"; bgColor = "#DBEAFE"; break;
            default:         emoji = "❓"; color = "#6B7280"; bgColor = "#F3F4F6";
        }
        Label badge = new Label(emoji + " " + (statut != null ? statut : "inconnu"));
        badge.setStyle("-fx-background-color:" + bgColor + ";-fx-text-fill:" + color + ";"
                + "-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10 3 10;"
                + "-fx-background-radius:20px;");
        return badge;
    }

    private long calculateDuration(Entretien e) {
        if (e.getHeureDebut() == null || e.getHeureFin() == null) return 0;
        return TimeUnit.MILLISECONDS.toMinutes(
                e.getHeureFin().getTime() - e.getHeureDebut().getTime());
    }

    private void openForm(boolean edition, Entretien ent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
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
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void rejoindre(Entretien e) {
        if (e.getDateEntretien() != null) {
            LocalDate dateE = e.getDateEntretien().toLocalDate();
            LocalDate auj   = LocalDate.now();
            if (!dateE.equals(auj)) {
                String msg = dateE.isAfter(auj)
                        ? "Cet entretien est prévu pour le "
                        + dateE.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ".\nVous ne pouvez rejoindre que le jour J."
                        : "Cet entretien était prévu pour le "
                        + dateE.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ".\nLa date est passée.";
                showAlert(Alert.AlertType.WARNING, "Accès impossible", msg);
                return;
            }
        }
        if ("visio".equals(e.getTypeEntretien())
                && e.getLienVisio() != null && !e.getLienVisio().isBlank()) {
            try {
                Desktop.getDesktop().browse(new URI(e.getLienVisio()));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible d'ouvrir le lien : " + ex.getMessage());
            }
        }
    }

    private void consulterMap(Entretien e) {
        if (!"présentiel".equals(e.getTypeEntretien())) return;
        if (e.getLieu() == null || e.getLieu().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Adresse manquante",
                    "Aucune adresse n'est définie pour cet entretien présentiel.");
            return;
        }
        try {
            String titrePoste = service.getOffreTitre(e.getIdOffre());
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/map-view.fxml"));
            Parent root = loader.load();
            MapController mapCtrl = loader.getController();
            mapCtrl.setAdresse(e.getLieu(), "📅 " + titrePoste);
            Stage mapStage = new Stage();
            mapStage.setTitle("JobNest – Carte : " + e.getLieu());
            mapStage.setScene(new Scene(root));
            mapStage.setMinWidth(700);
            mapStage.setMinHeight(500);
            mapStage.setWidth(960);
            mapStage.setHeight(680);
            mapStage.centerOnScreen();
            mapStage.show();
        } catch (IOException | SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la carte : " + ex.getMessage());
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
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Échec de la suppression : " + ex.getMessage());
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
    private void ouvrirProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/profil-recruteur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Mon Profil");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le profil : " + ex.getMessage());
        }
    }
    @FXML
    private void ouvrirMatching(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/matching-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Matching");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le matching : " + ex.getMessage());
        }
    }
}