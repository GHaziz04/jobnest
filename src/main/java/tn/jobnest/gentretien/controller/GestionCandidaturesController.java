package tn.jobnest.gentretien.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.service.CandidatureService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GestionCandidaturesController {

    @FXML private VBox vboxCandidatures;
    @FXML private Label totalCandidaturesLabel;
    @FXML private Label boostedLabel;
    @FXML private Label enAttenteLabel;
    @FXML private TextField searchField;

    private final CandidatureService service = new CandidatureService();
    private final int CURRENT_RECRUTEUR_ID = 1;

    /** Uniquement les candidatures "en_attente" */
    private List<CandidatureDTO> candidaturesEnAttente;

    @FXML
    public void initialize() {
        chargerDonnees();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerCandidatures(newVal));
        }
    }

    // ------------------------------------------------------------------ //
    //  CHARGEMENT — uniquement en_attente
    // ------------------------------------------------------------------ //
    private void chargerDonnees() {
        List<CandidatureDTO> toutes = service.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);

        // On garde UNIQUEMENT les en_attente pour l'affichage
        candidaturesEnAttente = toutes.stream()
                .filter(c -> "en_attente".equalsIgnoreCase(c.getStatut())
                        || "en attente".equalsIgnoreCase(c.getStatut()))
                .collect(Collectors.toList());

        mettreAJourStats(candidaturesEnAttente);
        afficherCandidatures(candidaturesEnAttente);
    }

    @FXML
    private void rafraichirListe() {
        chargerDonnees();
    }

    // ------------------------------------------------------------------ //
    //  STATS (sur les en_attente uniquement)
    // ------------------------------------------------------------------ //
    private void mettreAJourStats(List<CandidatureDTO> liste) {
        // Total en attente
        totalCandidaturesLabel.setText(String.valueOf(liste.size()));
        // Profils boostés parmi les en attente
        boostedLabel.setText(String.valueOf(liste.stream().filter(CandidatureDTO::isBoosted).count()));
        // Sans entretien planifié (approximation : tous pour l'instant, à affiner si besoin)
        enAttenteLabel.setText(String.valueOf(liste.size()));
    }

    // ------------------------------------------------------------------ //
    //  FILTRE PAR RECHERCHE TEXTE
    // ------------------------------------------------------------------ //
    private void filtrerCandidatures(String query) {
        if (query == null || query.isEmpty()) {
            afficherCandidatures(candidaturesEnAttente);
            return;
        }
        String q = query.toLowerCase();
        List<CandidatureDTO> filtrees = candidaturesEnAttente.stream()
                .filter(c -> c.getNomComplet().toLowerCase().contains(q)
                        || c.getTitreOffre().toLowerCase().contains(q))
                .collect(Collectors.toList());
        afficherCandidatures(filtrees);
    }

    // ------------------------------------------------------------------ //
    //  AFFICHAGE
    // ------------------------------------------------------------------ //
    private void afficherCandidatures(List<CandidatureDTO> liste) {
        vboxCandidatures.getChildren().clear();
        if (liste.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label emptyIcon = new Label("📭");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyText = new Label("Aucune candidature en attente");
            emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
            Label emptySubText = new Label("Toutes les candidatures ont été traitées !");
            emptySubText.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1;");
            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptySubText);
            vboxCandidatures.getChildren().add(emptyBox);
        } else {
            for (CandidatureDTO dto : liste) {
                vboxCandidatures.getChildren().add(creerItemCandidature(dto));
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  CRÉATION CARTE CANDIDATURE
    // ------------------------------------------------------------------ //
    private HBox creerItemCandidature(CandidatureDTO dto) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 18 22 18 22;"
                + "-fx-background-color: white;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.07), 12, 0, 0, 4);"
                + (dto.isBoosted()
                ? "-fx-border-color: #7C3AED; -fx-border-width: 0 0 0 5; -fx-border-radius: 0 14 14 0;"
                : "-fx-border-color: #EFF3FB; -fx-border-width: 1; -fx-border-radius: 14;"));

        // Avatar initiales
        String nomComplet = dto.getNomComplet();
        String[] parts = nomComplet.split(" ");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))
                : nomComplet.substring(0, Math.min(2, nomComplet.length()));
        initials = initials.toUpperCase();

        StackPane avatarStack = new StackPane();
        Label avatarLabel = new Label(initials);
        avatarLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
        Region avatarBg = new Region();
        avatarBg.setPrefSize(48, 48);
        avatarBg.setStyle("-fx-background-color: linear-gradient(135deg, #2563EB 0%, #1E40AF 100%);"
                + "-fx-background-radius: 50;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 8, 0, 0, 2);");
        avatarStack.getChildren().addAll(avatarBg, avatarLabel);
        avatarStack.setPrefSize(48, 48);
        avatarStack.setMaxSize(48, 48);
        avatarStack.setMinSize(48, 48);

        if (dto.isBoosted()) {
            Label boostBadge = new Label("⚡");
            boostBadge.setStyle("-fx-font-size: 10px; -fx-background-color: #7C3AED;"
                    + "-fx-background-radius: 50; -fx-padding: 1 3; -fx-text-fill: white;");
            boostBadge.setTranslateX(16);
            boostBadge.setTranslateY(-16);
            avatarStack.getChildren().add(boostBadge);
        }

        // Infos candidat
        VBox colCandidat = new VBox(4);
        colCandidat.setPrefWidth(210);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1E3A5F;");
        Label lblTitrePro = new Label(dto.getTitrePro() != null && !dto.getTitrePro().isEmpty()
                ? dto.getTitrePro() : "Candidat");
        lblTitrePro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 500;");
        colCandidat.getChildren().addAll(lblNom, lblTitrePro);

        // Infos offre
        VBox colOffre = new VBox(4);
        colOffre.setPrefWidth(210);
        Label lblOffreTitre = new Label("POSTULÉ POUR");
        lblOffreTitre.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 0 0 1 0;");
        Label lblOffreNom = new Label(dto.getTitreOffre());
        lblOffreNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        colOffre.getChildren().addAll(lblOffreTitre, lblOffreNom);

        // Badge statut — toujours "en attente" dans cette vue
        Label lblStatut = new Label("⏳ EN ATTENTE");
        lblStatut.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;"
                + "-fx-padding: 5 14; -fx-background-radius: 20;"
                + "-fx-font-weight: 700; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Bouton Traiter
        Button btnTraiter = new Button("✓ Traiter");
        btnTraiter.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;"
                + "-fx-font-weight: 700; -fx-font-size: 12px;"
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 8, 0, 0, 2);");
        btnTraiter.setOnAction(e -> {
            if (service.modifierStatut(dto.getIdCandidature(), "Traité")) {
                rafraichirListe();
            }
        });

        // Bouton Détails
        Button btnDetails = new Button("📄 Détails");
        btnDetails.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569;"
                + "-fx-font-weight: 700; -fx-font-size: 12px;"
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;");
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));

        // Bouton Planifier entretien
        Button btnEntretien = new Button("📅 Entretien");
        btnEntretien.setStyle("-fx-background-color: #1E3A5F; -fx-text-fill: white;"
                + "-fx-font-weight: 700; -fx-font-size: 12px;"
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);");
        btnEntretien.setOnAction(e -> planifierEntretien(dto, e));

        actions.getChildren().addAll(btnTraiter, btnDetails, btnEntretien);
        row.getChildren().addAll(avatarStack, colCandidat, colOffre, lblStatut, spacer, actions);
        return row;
    }

    // ------------------------------------------------------------------ //
    //  ACTIONS
    // ------------------------------------------------------------------ //
    private void ouvrirDetails(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/candidature-details.fxml"));
            Parent root = loader.load();
            CandidatureDetailsController controller = loader.getController();
            controller.chargerDonnees(dto);
            Stage popupStage = new Stage();
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.setTitle("Documents de " + dto.getNomComplet());
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
        } catch (IOException e) {
            showError("Impossible d'ouvrir les détails : " + e.getMessage());
        }
    }

    private void planifierEntretien(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Parent root = loader.load();
            switchScene(event, root);
        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire d'entretien : " + e.getMessage());
        }
    }

    private void switchScene(ActionEvent event, Parent root) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
            scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    // ------------------------------------------------------------------ //
    //  NAVIGATION SIDEBAR + BOUTON HISTORIQUE
    // ------------------------------------------------------------------ //

    /** Ouvre l'historique des candidatures (traitées) */
    @FXML
    private void ouvrirHistoriqueCandidatures(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/HistoriqueCandidatures.fxml", "JobNest - Historique des Candidatures");
    }

    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/entretien-view.fxml", "JobNest - Gestion des Entretiens");
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/feedback-interface.fxml", "JobNest - Gestion des Feedbacks");
    }

    private void naviguer(ActionEvent event, String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("JobNest");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}