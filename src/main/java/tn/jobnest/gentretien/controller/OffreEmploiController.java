package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.service.OffreEmploiService;
import tn.jobnest.gentretien.service.OffreAlertService;

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
    @FXML private ComboBox<String> statutFilterBox;
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

        statutFilterBox.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Ouvertes", "Fermées"));
        statutFilterBox.setValue("Tous les statuts");

        chargerOffres();

        searchField.textProperty()   .addListener((obs, o, n) -> appliquerFiltres());
        contratFilterBox.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        statutFilterBox.valueProperty() .addListener((obs, o, n) -> appliquerFiltres());
    }

    @FXML
    private void actualiser() {
        chargerOffres();
    }

    private void chargerOffres() {
        try {
            masterData   = FXCollections.observableArrayList(service.getOffres());
            filteredData = new FilteredList<>(masterData, p -> true);
            appliquerFiltres();

            Thread alertThread = new Thread(() -> {
                try {
                    new OffreAlertService().verifierEtEnvoyerAlertes();
                } catch (Exception e) {
                    System.err.println("[Alertes] Erreur : " + e.getMessage());
                }
            });
            alertThread.setDaemon(true);
            alertThread.setName("Offre-Alert-Thread");
            alertThread.start();

        } catch (SQLException e) {
            showAlert("Erreur chargement des offres : " + e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (filteredData == null) return;

        String keyword = searchField.getText() == null
                ? "" : searchField.getText().toLowerCase();
        String contrat = contratFilterBox.getValue();
        String statut  = statutFilterBox.getValue();

        filteredData.setPredicate(o -> {
            boolean matchText = keyword.isEmpty()
                    || (o.getTitre()      != null && o.getTitre().toLowerCase().contains(keyword))
                    || (o.getEntreprise() != null && o.getEntreprise().toLowerCase().contains(keyword));

            boolean matchContrat = "Tous".equals(contrat)
                    || (o.getTypeContrat() != null
                    && o.getTypeContrat().equalsIgnoreCase(contrat));

            boolean matchStatut;
            if ("Ouvertes".equals(statut)) {
                matchStatut = isOffreOuverte(o);
            } else if ("Fermées".equals(statut)) {
                matchStatut = !isOffreOuverte(o);
            } else {
                matchStatut = true;
            }

            return matchText && matchContrat && matchStatut;
        });

        int total     = filteredData.size();
        int pageCount = Math.max((int) Math.ceil((double) total / ITEMS_PER_PAGE), 1);

        if (tableCountLabel != null)
            tableCountLabel.setText(total + " offre" + (total > 1 ? "s" : ""));

        pagination.setPageCount(pageCount);
        pagination.setPageFactory(this::buildPage);
    }

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
        return new VBox();
    }

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
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-color: #BFDBFE;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-padding: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.18), 18, 0, 0, 6);" +
                        "-fx-translate-y: -4px;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-padding: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"));

        boolean ouvert = isOffreOuverte(o);

        Label statusBadge = new Label(ouvert ? "● Ouvert" : "● Fermé");
        statusBadge.setStyle(ouvert
                ? "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A; -fx-font-size:11px;" +
                "-fx-font-weight:bold; -fx-background-radius:20; -fx-padding:3 10 3 10;"
                : "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626; -fx-font-size:11px;" +
                "-fx-font-weight:bold; -fx-background-radius:20; -fx-padding:3 10 3 10;");

        Label titre = new Label(o.getTitre() != null ? o.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1E3A5F;");
        titre.setWrapText(true);

        Label entreprise = new Label("🏢  " + (o.getEntreprise() != null ? o.getEntreprise() : "—"));
        entreprise.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");

        Label contrat = new Label("📄  " + (o.getTypeContrat() != null ? o.getTypeContrat() : "—"));
        contrat.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        double sMin = o.getSalaireMin(), sMax = o.getSalaireMax();
        String salaireStr;
        if (sMin > 0 && sMax > 0 && sMin != sMax)
            salaireStr = "💰  " + (int) sMin + " – " + (int) sMax + " DT";
        else if (sMin > 0)
            salaireStr = "💰  " + (int) sMin + " DT";
        else
            salaireStr = "💰  Non précisé";
        Label salaire = new Label(salaireStr);
        salaire.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        double score = o.getMatchingScore() != null ? o.getMatchingScore() : 0;
        Label scoreLabel = new Label("🎯  Matching : " + String.format("%.1f", score) + "%");
        scoreLabel.setStyle(
                "-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8;" +
                        "-fx-font-size:12px; -fx-font-weight:bold;" +
                        "-fx-background-radius:8; -fx-padding:4 10 4 10;");

        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(6); tagsPane.setVgap(6);
        if (o.getCompetences() != null) {
            for (Competence c : o.getCompetences()) {
                if (c == null || c.getNom() == null) continue;
                Label tag = new Label(c.getNom());
                tag.setStyle(
                        "-fx-background-color:#DBEAFE; -fx-text-fill:#1E40AF;" +
                                "-fx-font-size:11px; -fx-background-radius:12; -fx-padding:3 8 3 8;");
                tagsPane.getChildren().add(tag);
            }
        }
        if (o.getExperiences() != null) {
            for (Experience exp : o.getExperiences()) {
                if (exp == null || exp.getNom() == null) continue;
                Label tag = new Label(exp.getNom());
                tag.setStyle(
                        "-fx-background-color:#F0FDF4; -fx-text-fill:#15803D;" +
                                "-fx-font-size:11px; -fx-background-radius:12; -fx-padding:3 8 3 8;");
                tagsPane.getChildren().add(tag);
            }
        }

        Separator sep = new Separator();

        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle(
                "-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8;" +
                        "-fx-font-weight:bold; -fx-background-radius:8;" +
                        "-fx-cursor:hand; -fx-padding:6 12 6 12;");
        detailsBtn.setOnAction(e -> showDetails(o));

        Button editBtn = new Button("Modifier");
        editBtn.setStyle(
                "-fx-background-color:#2563EB; -fx-text-fill:white;" +
                        "-fx-font-weight:bold; -fx-background-radius:8;" +
                        "-fx-cursor:hand; -fx-padding:6 12 6 12;");

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle(
                "-fx-background-color:#EF4444; -fx-text-fill:white;" +
                        "-fx-font-weight:bold; -fx-background-radius:8;" +
                        "-fx-cursor:hand; -fx-padding:6 12 6 12;");
        deleteBtn.setOnAction(e -> confirmerSuppression(o));

        Button fermerBtn    = buildFermerButton(o, ouvert);
        Button republierBtn = buildRepublierButton(o, ouvert);

        if (!ouvert) {
            editBtn.setDisable(true);
            editBtn.setOpacity(0.45);
            editBtn.setTooltip(new Tooltip(
                    "Offre fermée — utilisez « Republier » pour la rouvrir"));
        } else {
            editBtn.setOnAction(e -> {
                offreEnEdition = o;
                isEditMode     = true;
                showAddForm();
            });
        }

        HBox row1 = new HBox(8, detailsBtn, editBtn);
        row1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox row2 = new HBox(8,
                ouvert ? fermerBtn : republierBtn,
                deleteBtn);
        row2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox actionsBox = new VBox(6, row1, row2);

        card.getChildren().addAll(
                statusBadge, titre, entreprise, contrat,
                salaire, scoreLabel, tagsPane, sep, actionsBox);
        return card;
    }

    private Button buildFermerButton(OffreEmploi o, boolean ouvert) {
        Button btn = new Button("🔒  Fermer l'offre");
        if (ouvert) {
            btn.setStyle(
                    "-fx-background-color:#FFF7ED; -fx-text-fill:#C2410C;" +
                            "-fx-font-weight:bold; -fx-border-color:#FED7AA;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color:#FFEDD5; -fx-text-fill:#9A3412;" +
                            "-fx-font-weight:bold; -fx-border-color:#FB923C;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color:#FFF7ED; -fx-text-fill:#C2410C;" +
                            "-fx-font-weight:bold; -fx-border-color:#FED7AA;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;"));
            btn.setOnAction(e -> confirmerFermeture(o));
        } else {
            btn.setVisible(false);
            btn.setManaged(false);
        }
        return btn;
    }

    private Button buildRepublierButton(OffreEmploi o, boolean ouvert) {
        Button btn = new Button("🔄  Republier");
        if (!ouvert) {
            btn.setStyle(
                    "-fx-background-color:#F0FDF4; -fx-text-fill:#15803D;" +
                            "-fx-font-weight:bold; -fx-border-color:#86EFAC;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color:#DCFCE7; -fx-text-fill:#14532D;" +
                            "-fx-font-weight:bold; -fx-border-color:#4ADE80;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color:#F0FDF4; -fx-text-fill:#15803D;" +
                            "-fx-font-weight:bold; -fx-border-color:#86EFAC;" +
                            "-fx-border-width:1.5; -fx-border-radius:8;" +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 12 6 12;"));
            btn.setOnAction(e -> lancerRepublication(o));
        } else {
            btn.setVisible(false);
            btn.setManaged(false);
        }
        return btn;
    }

    private void confirmerFermeture(OffreEmploi o) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Fermer l'offre");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(
                "Fermer « " + o.getTitre() + " » ?\n\n" +
                        "Les candidats ne pourront plus postuler à cette offre.\n" +
                        "Vous pouvez la republier ultérieurement.");

        ButtonType btnFermer  = new ButtonType("🔒  Fermer l'offre", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnFermer, btnAnnuler);
        alert.getDialogPane().lookupButton(btnFermer).setStyle(
                "-fx-background-color:#EA580C; -fx-text-fill:white;" +
                        "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnFermer) {
            try {
                service.fermerOffre(o.getIdOffre());
                chargerOffres();

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Offre fermée");
                ok.setHeaderText(null);
                ok.setContentText("✅  L'offre « " + o.getTitre() + " » a été fermée avec succès.");
                ok.showAndWait();

            } catch (SQLException e) {
                showAlert("Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }

    private void lancerRepublication(OffreEmploi o) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_form.fxml"));
            Parent root = loader.load();
            OffreFormController ctrl = loader.getController();

            OffreEmploi copie = clonerPourRepublication(o);
            ctrl.setOffre(copie);
            ctrl.setModeRepublication(true);

            Stage stage = new Stage();
            stage.setTitle("🔄 Republier — " + o.getTitre());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            OffreEmploi offreModifiee = ctrl.getOffre();
            if (offreModifiee != null) {
                offreModifiee.setStatut("publiee");
                service.modifierOffre(
                        offreModifiee,
                        ctrl.getSelectedCompetences(),
                        ctrl.getSelectedExperiences());
                chargerOffres();

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Republié !");
                ok.setHeaderText(null);
                ok.setGraphic(null);
                ok.setContentText("✅  L'offre « " + offreModifiee.getTitre() + " » est à nouveau ouverte aux candidatures !");
                ok.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de la republication : " + e.getMessage());
        }
    }

    private OffreEmploi clonerPourRepublication(OffreEmploi src) {
        OffreEmploi c = new OffreEmploi();
        c.setIdOffre(src.getIdOffre());
        c.setIdRecruteur(src.getIdRecruteur());
        c.setTitre(src.getTitre());
        c.setEntreprise(src.getEntreprise());
        c.setTypeContrat(src.getTypeContrat());
        c.setDescription(src.getDescription());
        c.setSalaireMin(src.getSalaireMin());
        c.setSalaireMax(src.getSalaireMax());
        c.setNbPostes(src.getNbPostes());
        c.setNiveauExperience(src.getNiveauExperience());
        c.setDatePublication(src.getDatePublication());
        c.setMatchingScore(src.getMatchingScore());
        c.setCompetences(src.getCompetences());
        c.setExperiences(src.getExperiences());
        c.setDateExpiration(null);
        c.setStatut("publiee");
        return c;
    }

    private boolean isOffreOuverte(OffreEmploi o) {
        if ("fermee".equals(o.getStatut())) return false;
        if (o.getDateExpiration() == null)   return true;
        return o.getDateExpiration().toLocalDate().isAfter(LocalDate.now());
    }

    private void playCardAnimation(VBox card) {
        FadeTransition  fade  = new FadeTransition(Duration.millis(280), card);
        fade.setFromValue(0); fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(280), card);
        scale.setFromX(0.95); scale.setFromY(0.95);
        scale.setToX(1);      scale.setToY(1);
        fade.play(); scale.play();
    }

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

            chargerOffres();
        } catch (Exception e) {
            showAlert("Erreur ouverture détails : " + e.getMessage());
        }
    }

    @FXML
    private void showAddForm() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/offre_form.fxml"));
            Parent root = loader.load();
            OffreFormController ctrl = loader.getController();

            if (isEditMode && offreEnEdition != null)
                ctrl.setOffre(offreEnEdition);

            Stage stage = new Stage();
            stage.setTitle(isEditMode ? "Modifier l'offre" : "Nouvelle Offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            OffreEmploi offre = ctrl.getOffre();
            if (offre != null) {
                List<Competence> competences = ctrl.getSelectedCompetences();
                List<Experience> experiences = ctrl.getSelectedExperiences();
                if (isEditMode)
                    service.modifierOffre(offre, competences, experiences);
                else
                    service.ajouterOffre(offre, competences, experiences);
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

    private void confirmerSuppression(OffreEmploi o) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(
                "Supprimer « " + o.getTitre() + " » ?\n" +
                        "Cette action est irréversible.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimerOffre(o.getIdOffre());
                chargerOffres();
            } catch (SQLException e) {
                showAlert(e.getMessage());
            }
        }
    }

    // ============================
    //  NAVIGATION SIDEBAR COMPLÈTE
    // ============================
    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/entretien-view.fxml",
                "JobNest - Gestion des Entretiens");
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/feedback-interface.fxml",
                "JobNest - Gestion des Feedbacks");
    }

    @FXML
    private void ouvrirHistorique(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/historique-entretien.fxml",
                "JobNest - Historique des Entretiens");
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/GestionCandidatures.fxml",
                "JobNest - Gestion des Candidatures");
    }

    @FXML
    private void ouvrirProfil(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/profil-recruteur.fxml",
                "JobNest - Mon Profil");
    }

    @FXML
    private void ouvrirMatching(ActionEvent event) {
        navigate(event, "/tn/jobnest/gentretien/matching-view.fxml",
                "JobNest - Matching");
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

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}