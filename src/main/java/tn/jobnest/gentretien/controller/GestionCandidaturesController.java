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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.service.CandidatureService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GestionCandidaturesController {

    @FXML private VBox vboxCandidatures;
    @FXML private Label totalCandidaturesLabel;
    @FXML private Label boostedLabel;
    @FXML private Label enAttenteLabel;
    @FXML private TextField searchField;

    private final CandidatureService service = new CandidatureService();
    private final int CURRENT_RECRUTEUR_ID = 1;

    private List<CandidatureDTO> candidaturesEnAttente;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        chargerDonnees();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerCandidatures(newVal));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        List<CandidatureDTO> toutes = service.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);

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

    // ─────────────────────────────────────────────────────────────────────────
    private void mettreAJourStats(List<CandidatureDTO> liste) {
        totalCandidaturesLabel.setText(String.valueOf(liste.size()));
        boostedLabel.setText(String.valueOf(liste.stream().filter(CandidatureDTO::isBoosted).count()));
        enAttenteLabel.setText(String.valueOf(liste.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    private VBox creerItemCandidature(CandidatureDTO dto) {

        boolean hasEntretien = service.candidatADejaUnEntretienPourOffre(
                dto.getIdCandidat(), dto.getIdOffre());

        VBox card = new VBox(14);
        card.setStyle(
                "-fx-padding: 20 24 20 24;"
                        + "-fx-background-color: white;"
                        + "-fx-background-radius: 16;"
                        + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.09), 14, 0, 0, 5);"
                        + (dto.isBoosted()
                        ? "-fx-border-color: #7C3AED; -fx-border-width: 0 0 0 6; -fx-border-radius: 0 16 16 0;"
                        : "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 16;"));

        HBox topRow = new HBox(18);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String nomComplet = dto.getNomComplet();
        String[] parts = nomComplet.split(" ");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))
                : nomComplet.substring(0, Math.min(2, nomComplet.length()));
        initials = initials.toUpperCase();

        javafx.scene.layout.StackPane avatarStack = new javafx.scene.layout.StackPane();
        Region avatarBg = new Region();
        avatarBg.setPrefSize(58, 58);
        avatarBg.setStyle(
                "-fx-background-color: " + (hasEntretien
                        ? "linear-gradient(from 0% 0% to 100% 100%, #2563EB, #1E40AF)"
                        : "linear-gradient(from 0% 0% to 100% 100%, #F97316, #EA580C)") + ";"
                        + "-fx-background-radius: 50;"
                        + "-fx-effect: dropshadow(gaussian, "
                        + (hasEntretien ? "rgba(37,99,235,0.4)" : "rgba(249,115,22,0.4)")
                        + ", 10, 0, 0, 3);");
        Label avatarLabel = new Label(initials);
        avatarLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: white;");
        avatarStack.getChildren().addAll(avatarBg, avatarLabel);
        avatarStack.setPrefSize(58, 58);
        avatarStack.setMaxSize(58, 58);
        avatarStack.setMinSize(58, 58);

        if (dto.isBoosted()) {
            Label boostBadge = new Label("⚡");
            boostBadge.setStyle(
                    "-fx-font-size: 11px; -fx-background-color: #7C3AED;"
                            + "-fx-background-radius: 50; -fx-padding: 2 4; -fx-text-fill: white;");
            boostBadge.setTranslateX(22);
            boostBadge.setTranslateY(-22);
            avatarStack.getChildren().add(boostBadge);
        }

        VBox infoBlock = new VBox(6);
        HBox.setHgrow(infoBlock, javafx.scene.layout.Priority.ALWAYS);

        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 17px; -fx-text-fill: #1E3A5F;");

        Label lblTitrePro = new Label(
                dto.getTitrePro() != null && !dto.getTitrePro().isEmpty()
                        ? dto.getTitrePro() : "Candidat");
        lblTitrePro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-font-weight: 500;");

        HBox offreRow = new HBox(6);
        offreRow.setAlignment(Pos.CENTER_LEFT);
        Label lblOffreLabel = new Label("Postulé pour :");
        lblOffreLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");
        Label lblOffreNom = new Label(dto.getTitreOffre());
        lblOffreNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        offreRow.getChildren().addAll(lblOffreLabel, lblOffreNom);
        infoBlock.getChildren().addAll(lblNom, lblTitrePro, offreRow);

        VBox badgesBlock = new VBox(6);
        badgesBlock.setAlignment(Pos.CENTER_RIGHT);

        Label lblStatut = new Label("⏳  EN ATTENTE");
        lblStatut.setStyle(
                "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;"
                        + "-fx-padding: 7 16; -fx-background-radius: 20;"
                        + "-fx-font-weight: 800; -fx-font-size: 12px;");

        Label lblEntretien = hasEntretien
                ? new Label("📅 Entretien planifié")
                : new Label("⏳ Sans entretien");
        lblEntretien.setStyle(hasEntretien
                ? "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;"
                + "-fx-padding:7 16; -fx-background-radius:20;"
                + "-fx-font-weight:700; -fx-font-size:12px;"
                : "-fx-background-color:#FEF9C3; -fx-text-fill:#D97706;"
                + "-fx-padding:7 16; -fx-background-radius:20;"
                + "-fx-font-weight:700; -fx-font-size:12px;");

        badgesBlock.getChildren().addAll(lblStatut, lblEntretien);
        topRow.getChildren().addAll(avatarStack, infoBlock, badgesBlock);

        Separator separator = new Separator();

        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnTraiter = new Button("✓   Marquer comme Traité");
        btnTraiter.setPrefHeight(44);
        btnTraiter.setMinWidth(210);
        btnTraiter.setStyle(
                "-fx-background-color: #2563EB;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: 800;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 10 22;"
                        + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.35), 10, 0, 0, 3);");
        btnTraiter.setOnAction(e -> {
            if (service.modifierStatut(dto.getIdCandidature(), "traité")) {
                rafraichirListe();
            }
        });

        Button btnDetails = new Button("📄   Voir Détails");
        btnDetails.setPrefHeight(44);
        btnDetails.setMinWidth(150);
        btnDetails.setStyle(
                "-fx-background-color: #F1F5F9;"
                        + "-fx-text-fill: #1E3A5F;"
                        + "-fx-font-weight: 800;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 10 22;"
                        + "-fx-border-color: #CBD5E1;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1.5;");
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));

        Button btnSupprimer = new Button("🗑   Supprimer");
        btnSupprimer.setPrefHeight(44);
        btnSupprimer.setMinWidth(140);
        btnSupprimer.setStyle(
                "-fx-background-color: #FEF2F2;"
                        + "-fx-text-fill: #DC2626;"
                        + "-fx-font-weight: 800;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 10 22;"
                        + "-fx-border-color: #FECACA;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1.5;");
        btnSupprimer.setOnMouseEntered(e -> btnSupprimer.setStyle(
                "-fx-background-color: #DC2626;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: 800;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 10 22;"
                        + "-fx-border-color: #DC2626;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.35), 10, 0, 0, 3);"));
        btnSupprimer.setOnMouseExited(e -> btnSupprimer.setStyle(
                "-fx-background-color: #FEF2F2;"
                        + "-fx-text-fill: #DC2626;"
                        + "-fx-font-weight: 800;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 10 22;"
                        + "-fx-border-color: #FECACA;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1.5;"));
        btnSupprimer.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmer la suppression");
            confirmation.setHeaderText("Supprimer la candidature de " + dto.getNomComplet() + " ?");
            confirmation.setContentText(
                    "La candidature sera marquée comme « annulé » et ne sera plus affichée.\n"
                            + "Elle restera enregistrée dans la base de données.");
            ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmation.getButtonTypes().setAll(btnOui, btnNon);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == btnOui) {
                if (service.modifierStatut(dto.getIdCandidature(), "annulé")) {
                    rafraichirListe();
                } else {
                    showError("Erreur lors de la suppression de la candidature.");
                }
            }
        });

        actionsRow.getChildren().addAll(btnTraiter, btnDetails, btnSupprimer);
        card.getChildren().addAll(topRow, separator, actionsRow);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void ouvrirDetails(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/candidature-details.fxml"));
            Parent root = loader.load();
            CandidatureDetailsController controller = loader.getController();
            controller.chargerDonnees(dto);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.setTitle("Documents de " + dto.getNomComplet());
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
        } catch (IOException e) {
            showError("Impossible d'ouvrir les détails : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION SIDEBAR
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirHistoriqueCandidatures(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/HistoriqueCandidatures.fxml",
                "JobNest - Historique des Candidatures");
    }

    @FXML
    private void ouvrirProfil(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/profil-recruteur.fxml", "JobNest - Mon Profil");
    }

    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/entretien-view.fxml",
                "JobNest - Gestion des Entretiens");
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/feedback-interface.fxml",
                "JobNest - Gestion des Feedbacks");
    }

    // ────────────────────────────────────────────────────────────────
    //  ✅ NAVIGATION VERS OFFRES D'EMPLOI (SIDEBAR)
    // ────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirOffresEmploi(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/offre-emploi_view.fxml",
                "JobNest - Offres d'Emploi");
    }

    private void naviguer(ActionEvent event, String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("JobNest");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}