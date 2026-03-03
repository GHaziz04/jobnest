package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.service.OffreEmploiService;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class OffreEmploiController {

    // ===== FXML =====
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> contratFilterBox;
    @FXML private FlowPane         cardContainer;
    @FXML private Label            tableCountLabel;
    @FXML private Pagination       pagination;

    // ===== DATA =====
    private static final int ITEMS_PER_PAGE = 6;
    private OffreEmploiService          service;
    private ObservableList<OffreEmploi> masterData;
    private FilteredList<OffreEmploi>   filteredData;

    // ===== EDIT STATE =====
    private boolean     isEditMode     = false;
    private OffreEmploi offreEnEdition = null;

    // ============================
    //  INITIALIZE
    // ============================
    @FXML
    public void initialize() {
        service = new OffreEmploiService();

        contratFilterBox.setItems(FXCollections.observableArrayList(
                "Tous", "CDI", "CDD", "Stage", "Alternance", "Freelance"));
        contratFilterBox.setValue("Tous");

        chargerOffres();

        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());
        contratFilterBox.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
    }

    // ============================
    //  CHARGEMENT
    // ============================
    private void chargerOffres() {
        try {
            masterData   = FXCollections.observableArrayList(service.getOffres());
            filteredData = new FilteredList<>(masterData, p -> true);
            appliquerFiltres();
        } catch (SQLException e) {
            showAlert("Erreur chargement des offres : " + e.getMessage());
        }
    }

    // ============================
    //  FILTRES
    // ============================
    private void appliquerFiltres() {
        if (filteredData == null) return;

        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String contrat = contratFilterBox.getValue();

        filteredData.setPredicate(o -> {
            boolean matchText = keyword.isEmpty()
                    || (o.getTitre()      != null && o.getTitre().toLowerCase().contains(keyword))
                    || (o.getEntreprise() != null && o.getEntreprise().toLowerCase().contains(keyword));
            boolean matchContrat = "Tous".equals(contrat)
                    || (o.getTypeContrat() != null && o.getTypeContrat().equalsIgnoreCase(contrat));
            return matchText && matchContrat;
        });

        int total     = filteredData.size();
        int pageCount = Math.max((int) Math.ceil((double) total / ITEMS_PER_PAGE), 1);

        if (tableCountLabel != null)
            tableCountLabel.setText(total + " offre" + (total > 1 ? "s" : ""));

        pagination.setPageCount(pageCount);
        // ✅ FIX AFFICHAGE : setPageFactory peuple cardContainer via Platform.runLater
        pagination.setPageFactory(this::buildPage);
    }

    // ============================
    //  PAGE FACTORY
    //  ✅ FIX : retourne un VBox vide (requis par l'API Pagination)
    //  Les cartes sont injectées dans le FlowPane FXML via Platform.runLater
    // ============================
    private VBox buildPage(int pageIndex) {
        if (filteredData == null || cardContainer == null) return new VBox();

        Platform.runLater(() -> {
            cardContainer.getChildren().clear();

            if (filteredData.isEmpty()) {
                Label empty = new Label("Aucune offre disponible.");
                empty.setStyle("-fx-font-size:14px; -fx-text-fill:#94A3B8; -fx-padding:40;");
                cardContainer.getChildren().add(empty);
                return;
            }

            int from = pageIndex * ITEMS_PER_PAGE;
            int to   = Math.min(from + ITEMS_PER_PAGE, filteredData.size());

            for (int i = from; i < to; i++) {
                VBox card = createCard(filteredData.get(i));
                cardContainer.getChildren().add(card);
                playCardAnimation(card);
            }
        });

        return new VBox(); // requis par Pagination API
    }

    // ============================
    //  CARTE OFFRE
    // ============================
    private VBox createCard(OffreEmploi o) {
        VBox card = new VBox(10);
        card.getStyleClass().add("job-card");
        card.setPrefWidth(300);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-padding: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-color: #BFDBFE;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-padding: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.18), 18, 0, 0, 6);" +
                        "-fx-translate-y: -4px;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-padding: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        ));

        // Statut
        boolean ouvert = o.getDateExpiration() != null
                && o.getDateExpiration().toLocalDate().isAfter(LocalDate.now());

        Label statusBadge = new Label(ouvert ? "● Ouvert" : "● Fermé");
        statusBadge.setStyle(ouvert
                ? "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:3 10 3 10;"
                : "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626; -fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:3 10 3 10;");

        Label titre = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;");
        titre.setWrapText(true);

        Label entreprise = new Label("🏢  " + (o.getEntreprise() != null ? o.getEntreprise() : "—"));
        entreprise.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");

        Label contrat = new Label("📄  " + (o.getTypeContrat() != null ? o.getTypeContrat() : "—"));
        contrat.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        double sMin = o.getSalaireMin(), sMax = o.getSalaireMax();
        String salaireStr = (sMin > 0 || sMax > 0)
                ? "💰  " + (int)sMin + " – " + (int)sMax + " DT"
                : "💰  Non précisé";
        Label salaire = new Label(salaireStr);
        salaire.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        double score = o.getMatchingScore() != null ? o.getMatchingScore() : 0;
        Label scoreLabel = new Label("🎯  Matching : " + String.format("%.1f", score) + "%");
        scoreLabel.setStyle("-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8; -fx-font-size:12px; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:4 10 4 10;");

        // Tags compétences + expériences
        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(6); tagsPane.setVgap(6);

        if (o.getCompetences() != null) {
            for (Competence c : o.getCompetences()) {
                if (c == null || c.getNom() == null) continue;
                Label tag = new Label(c.getNom());
                tag.setStyle("-fx-background-color:#DBEAFE; -fx-text-fill:#1E40AF; -fx-font-size:11px; -fx-background-radius:12; -fx-padding:3 8 3 8;");
                tagsPane.getChildren().add(tag);
            }
        }
        if (o.getExperiences() != null) {
            for (Experience exp : o.getExperiences()) {
                if (exp == null || exp.getNom() == null) continue;
                Label tag = new Label(exp.getNom());
                tag.setStyle("-fx-background-color:#F0FDF4; -fx-text-fill:#15803D; -fx-font-size:11px; -fx-background-radius:12; -fx-padding:3 8 3 8;");
                tagsPane.getChildren().add(tag);
            }
        }

        Separator sep = new Separator();

        // Boutons
        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle("-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8; -fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;");
        detailsBtn.setOnAction(e -> showDetails(o));

        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color:#2563EB; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;");

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;");

        if (!ouvert) {
            editBtn.setDisable(true);
            editBtn.setOpacity(0.45);
            editBtn.setTooltip(new Tooltip("Offre expirée — modification impossible"));
        } else {
            editBtn.setOnAction(e -> {
                offreEnEdition = o;
                isEditMode     = true;
                showAddForm();
            });
        }
        deleteBtn.setOnAction(e -> confirmerSuppression(o));

        HBox actions = new HBox(8, detailsBtn, editBtn, deleteBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        card.getChildren().addAll(
                statusBadge, titre, entreprise, contrat,
                salaire, scoreLabel, tagsPane, sep, actions
        );
        return card;
    }

    // ============================
    //  ANIMATION
    // ============================
    private void playCardAnimation(VBox card) {
        FadeTransition  fade  = new FadeTransition(Duration.millis(280), card);
        fade.setFromValue(0); fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(280), card);
        scale.setFromX(0.95); scale.setFromY(0.95);
        scale.setToX(1);      scale.setToY(1);
        fade.play(); scale.play();
    }

    // ============================
    //  DÉTAILS
    // ============================
    private void showDetails(OffreEmploi o) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_details.fxml"));
            Parent root = loader.load();
            OffreDetailsController ctrl = loader.getController();
            ctrl.setOffre(o);
            Stage stage = new Stage();
            stage.setTitle("Détails — " + o.getTitre());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert("Erreur ouverture détails : " + e.getMessage());
        }
    }

    // ============================
    //  FORMULAIRE AJOUT / MODIF
    // ============================
    @FXML
    private void showAddForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_form.fxml"));
            Parent root = loader.load();
            OffreFormController ctrl = loader.getController();

            if (isEditMode && offreEnEdition != null) ctrl.setOffre(offreEnEdition);

            Stage stage = new Stage();
            stage.setTitle(isEditMode ? "Modifier l'offre" : "Nouvelle Offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            OffreEmploi offre = ctrl.getOffre();
            if (offre != null) {
                List<Competence> competences = ctrl.getSelectedCompetences();
                List<Experience> experiences = ctrl.getSelectedExperiences();
                if (isEditMode) service.modifierOffre(offre, competences, experiences);
                else            service.ajouterOffre(offre, competences, experiences);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur formulaire : " + e.getMessage());
        } finally {
            isEditMode     = false;
            offreEnEdition = null;
            chargerOffres();
        }
    }

    // ============================
    //  SUPPRIMER
    // ============================
    private void confirmerSuppression(OffreEmploi o) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation"); alert.setHeaderText(null);
        alert.setContentText("Supprimer « " + o.getTitre() + " » ? Cette action est irréversible.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimerOffre(o.getIdOffre());
                chargerOffres();
            } catch (SQLException e) { showAlert(e.getMessage()); }
        }
    }

    // ============================
    //  NAVIGATION
    // ============================
    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/entretien-view.fxml",
                "JobNest - Gestion des Entretiens");
    }

    private void navigate(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root  = loader.load();
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene  scene = new Scene(root);
            URL    css   = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException ex) {
            showAlert("Erreur navigation : " + ex.getMessage());
        }
    }

    // ============================
    //  UTILS
    // ============================
    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}