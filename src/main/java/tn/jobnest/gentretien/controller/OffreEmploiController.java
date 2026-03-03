package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.service.OffreEmploiService;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class OffreEmploiController {

    // ================= UI =================
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> contratFilterBox;
    @FXML private FlowPane         cardContainer;
    @FXML private Label            tableCountLabel;
    @FXML private Pagination       pagination;

    // ================= DATA =================
    private static final int ITEMS_PER_PAGE = 6;
    private OffreEmploiService          service;
    private ObservableList<OffreEmploi> data;
    private FilteredList<OffreEmploi>   filteredData;

    // ================= EDIT STATE =================
    private boolean     isEditMode     = false;
    private OffreEmploi offreEnEdition = null;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        service = new OffreEmploiService();

        contratFilterBox.setItems(FXCollections.observableArrayList(
                "Tous", "CDI", "CDD", "Stage", "Freelance"
        ));
        contratFilterBox.setValue("Tous");

        // ✅ Charger d'abord les donnees AVANT tout listener
        chargerOffres();

        // ✅ Listeners APRES initialisation de filteredData
        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());
        contratFilterBox.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
    }

    // ================= LOAD =================
    private void chargerOffres() {
        try {
            data         = FXCollections.observableArrayList(service.getOffres());
            filteredData = new FilteredList<>(data, p -> true);
            appliquerFiltres();
        } catch (SQLException e) {
            showAlert("Erreur chargement : " + e.getMessage());
        }
    }

    // ================= FILTER =================
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
        int pageCount = (int) Math.ceil((double) total / ITEMS_PER_PAGE);

        tableCountLabel.setText(total + " offres");

        // ✅ setPageFactory() est la solution correcte :
        //    il ne declenche PAS currentPageIndexProperty avant que filteredData soit pret.
        //    Le callback n'est appele que quand JavaFX construit la page visuellement.
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setPageFactory(this::buildPage);
    }

    // ================= PAGE FACTORY =================
    private VBox buildPage(int pageIndex) {

        // Garde null absolue
        if (filteredData == null || cardContainer == null) return new VBox();

        cardContainer.getChildren().clear();

        int from = pageIndex * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, filteredData.size());

        if (from < to) {
            for (OffreEmploi o : filteredData.subList(from, to)) {
                VBox card = createCard(o);
                cardContainer.getChildren().add(card);
                playCardAnimation(card);
            }
        }

        // setPageFactory requiert un Node — on retourne VBox vide
        // Les cards sont inserees dans cardContainer (FlowPane du FXML)
        return new VBox();
    }

    // ================= CARD =================
    private VBox createCard(OffreEmploi o) {

        VBox card = new VBox(10);
        card.getStyleClass().add("job-card");
        card.setPrefWidth(280);

        card.setOnMouseEntered(e -> {
            card.setTranslateY(-6);
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0.2, 0, 6);");
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0.1, 0, 4);");
        });

        Label titre = new Label(o.getTitre() != null ? o.getTitre() : "");
        titre.getStyleClass().add("job-title");
        titre.setWrapText(true);

        Label entreprise = new Label(o.getEntreprise() != null ? o.getEntreprise() : "");
        entreprise.getStyleClass().add("company-name");

        double score = o.getMatchingScore() != null ? o.getMatchingScore() : 0;
        Label scoreLabel = new Label("Matching : " + String.format("%.1f", score) + " %");
        scoreLabel.getStyleClass().add("score-badge");

        Label contrat = new Label("Contrat : " + (o.getTypeContrat() != null ? o.getTypeContrat() : ""));
        Label salaire = new Label("Salaire : " + o.getSalaireMin() + " - " + o.getSalaireMax() + " DT");

        boolean ouvert = o.getDateExpiration() != null
                && o.getDateExpiration().toLocalDate().isAfter(LocalDate.now());

        Label statusBadge = new Label(ouvert ? "Ouvert" : "Ferme");
        statusBadge.getStyleClass().addAll("status-badge", ouvert ? "badge-open" : "badge-closed");

        // Competences tags
        FlowPane competencesBox = new FlowPane();
        competencesBox.setHgap(6);
        competencesBox.setVgap(6);
        competencesBox.setPrefWrapLength(250);
        if (o.getCompetences() != null) {
            for (Competence c : o.getCompetences()) {
                if (c != null && c.getNom() != null) {
                    Label tag = new Label(c.getNom());
                    tag.getStyleClass().add("competence-tag");
                    competencesBox.getChildren().add(tag);
                }
            }
        }

        // Experiences tags
        FlowPane experiencesBox = new FlowPane();
        experiencesBox.setHgap(6);
        experiencesBox.setVgap(6);
        experiencesBox.setPrefWrapLength(250);
        if (o.getExperiences() != null) {
            for (Experience exp : o.getExperiences()) {
                if (exp != null && exp.getNom() != null) {
                    Label tag = new Label(exp.getNom());
                    tag.getStyleClass().add("competence-tag");
                    experiencesBox.getChildren().add(tag);
                }
            }
        }

        // Boutons
        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-details");
        detailsBtn.setOnAction(e -> showDetails(o));

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("btn-edit");

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-delete");

        if (!ouvert) {
            editBtn.setDisable(true);
            editBtn.setOpacity(0.5);
            editBtn.setTooltip(new Tooltip("Offre expiree — modification impossible"));
        }

        editBtn.setOnAction(e -> {
            offreEnEdition = o;
            isEditMode     = true;
            showAddForm();
        });

        deleteBtn.setOnAction(e -> confirmerSuppression(o));

        HBox actions = new HBox(10, detailsBtn, editBtn, deleteBtn);
        actions.getStyleClass().add("card-actions");

        card.getChildren().addAll(
                titre, entreprise, scoreLabel,
                contrat, salaire, statusBadge,
                competencesBox, experiencesBox,
                actions
        );

        return card;
    }

    // ================= ANIMATION =================
    private void playCardAnimation(VBox card) {
        FadeTransition  fade  = new FadeTransition(Duration.millis(300), card);
        fade.setFromValue(0); fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), card);
        scale.setFromX(0.95); scale.setFromY(0.95);
        scale.setToX(1);     scale.setToY(1);
        fade.play(); scale.play();
    }

    // ================= DETAILS =================
    private void showDetails(OffreEmploi o) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_details.fxml"));
            Parent root = loader.load();

            OffreDetailsController ctrl = loader.getController();
            ctrl.setOffre(o);

            Stage stage = new Stage();
            stage.setTitle("Details de l'offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur ouverture details : " + e.getMessage());
        }
    }

    // ================= FORM ADD/EDIT =================
    @FXML
    private void showAddForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_form.fxml"));
            Parent root = loader.load();

            OffreFormController ctrl = loader.getController();

            if (isEditMode && offreEnEdition != null) {
                ctrl.setOffre(offreEnEdition);
            }

            Stage stage = new Stage();
            stage.setTitle(isEditMode ? "Modifier Offre" : "Nouvelle Offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            OffreEmploi offre = ctrl.getOffre();

            if (offre != null) {
                List<Competence> competences = ctrl.getSelectedCompetences();
                List<Experience> experiences = ctrl.getSelectedExperiences();

                if (isEditMode) {
                    service.modifierOffre(offre, competences, experiences);
                } else {
                    service.ajouterOffre(offre, competences, experiences);
                }

                isEditMode     = false;
                offreEnEdition = null;
                chargerOffres();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur formulaire : " + e.getMessage());
        }
    }

    // ================= DELETE =================
    private void confirmerSuppression(OffreEmploi o) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette offre ?");
        alert.setContentText(o.getTitre());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimerOffre(o.getIdOffre());
                chargerOffres();
                showSuccess("Offre supprimee !");
            } catch (SQLException e) {
                showAlert(e.getMessage());
            }
        }
    }

    // ================= UTILS =================
    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}