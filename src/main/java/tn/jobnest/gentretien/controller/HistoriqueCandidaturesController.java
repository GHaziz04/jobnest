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

public class HistoriqueCandidaturesController {

    @FXML private VBox vboxHistorique;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> comboEntretien;
    @FXML private Label totalTraitesLabel;
    @FXML private Label avecEntretienLabel;
    @FXML private Label sansEntretienLabel;
    @FXML private Label boostedTraitesLabel;

    private final CandidatureService candidatureService = new CandidatureService();
    private final int CURRENT_RECRUTEUR_ID = 1;

    /** Toutes les candidatures traitées */
    private List<CandidatureDTO> toutesTraitees;

    @FXML
    public void initialize() {
        comboEntretien.setItems(FXCollections.observableArrayList(
                "Tous", "Avec entretien", "Sans entretien"
        ));
        comboEntretien.setValue("Tous");

        comboEntretien.valueProperty().addListener((obs, old, nv) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, nv) -> filterAndDisplay());

        chargerDonnees();
    }

    // ------------------------------------------------------------------ //
    //  BOUTON ACTUALISER
    // ------------------------------------------------------------------ //
    @FXML
    private void rafraichirListe() {
        chargerDonnees();
    }

    // ------------------------------------------------------------------ //
    //  CHARGEMENT — uniquement les "traité"
    // ------------------------------------------------------------------ //
    private void chargerDonnees() {
        List<CandidatureDTO> toutes = candidatureService.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);

        toutesTraitees = toutes.stream()
                .filter(c -> "traité".equalsIgnoreCase(c.getStatut())
                        || "traite".equalsIgnoreCase(c.getStatut()))
                .collect(Collectors.toList());

        updateStats();
        filterAndDisplay();
    }

    // ------------------------------------------------------------------ //
    //  STATS
    // ------------------------------------------------------------------ //
    private void updateStats() {
        if (toutesTraitees == null) return;

        long total = toutesTraitees.size();
        long avecE = toutesTraitees.stream().filter(c -> aUnEntretien(c.getIdCandidat())).count();
        long sansE = total - avecE;
        long boosted = toutesTraitees.stream().filter(CandidatureDTO::isBoosted).count();

        totalTraitesLabel.setText(String.valueOf(total));
        avecEntretienLabel.setText(String.valueOf(avecE));
        sansEntretienLabel.setText(String.valueOf(sansE));
        boostedTraitesLabel.setText(String.valueOf(boosted));
    }

    // ------------------------------------------------------------------ //
    //  FILTRE + AFFICHAGE
    // ------------------------------------------------------------------ //
    private void filterAndDisplay() {
        vboxHistorique.getChildren().clear();

        if (toutesTraitees == null || toutesTraitees.isEmpty()) {
            afficherMessageVide("Aucune candidature traitée pour le moment.");
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selEntretien = comboEntretien.getValue();

        boolean auMoinsUn = false;

        for (CandidatureDTO dto : toutesTraitees) {
            boolean hasEntretien = aUnEntretien(dto.getIdCandidat());

            // Filtre texte
            boolean matchSearch = search.isEmpty()
                    || dto.getNomComplet().toLowerCase().contains(search)
                    || dto.getTitreOffre().toLowerCase().contains(search);

            // Filtre avec/sans entretien
            boolean matchEntretien;
            if ("Avec entretien".equals(selEntretien)) {
                matchEntretien = hasEntretien;
            } else if ("Sans entretien".equals(selEntretien)) {
                matchEntretien = !hasEntretien;
            } else {
                matchEntretien = true;
            }

            if (matchSearch && matchEntretien) {
                vboxHistorique.getChildren().add(creerItemHistorique(dto, hasEntretien));
                auMoinsUn = true;
            }
        }

        if (!auMoinsUn) {
            afficherMessageVide("Aucun résultat pour ces filtres.");
        }
    }

    private void afficherMessageVide(String message) {
        VBox emptyBox = new VBox(12);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));
        Label emptyIcon = new Label("📭");
        emptyIcon.setStyle("-fx-font-size: 48px;");
        Label emptyText = new Label(message);
        emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
        emptyBox.getChildren().addAll(emptyIcon, emptyText);
        vboxHistorique.getChildren().add(emptyBox);
    }

    // ------------------------------------------------------------------ //
    //  VÉRIFICATION ENTRETIEN
    //  Vérifie si le candidat est participant dans au moins un entretien
    // ------------------------------------------------------------------ //
    private boolean aUnEntretien(int idCandidat) {
        return candidatureService.candidatAUnEntretien(idCandidat);
    }

    // ------------------------------------------------------------------ //
    //  CRÉATION CARTE HISTORIQUE CANDIDATURE
    // ------------------------------------------------------------------ //
    private HBox creerItemHistorique(CandidatureDTO dto, boolean hasEntretien) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Bordure bleue = avec entretien, orange = sans entretien
        String couleurBordure = hasEntretien ? "#2563EB" : "#F97316";
        row.setStyle("-fx-padding: 18 22 18 22;"
                + "-fx-background-color: white;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.07), 12, 0, 0, 4);"
                + "-fx-border-color: " + couleurBordure + ";"
                + "-fx-border-width: 0 0 0 5;"
                + "-fx-border-radius: 0 14 14 0;");

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
        // Couleur avatar différente selon statut entretien
        String avatarColor = hasEntretien
                ? "linear-gradient(135deg, #2563EB 0%, #1E40AF 100%)"
                : "linear-gradient(135deg, #F97316 0%, #EA580C 100%)";
        avatarBg.setStyle("-fx-background-color: " + avatarColor + ";"
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
        colCandidat.setPrefWidth(200);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1E3A5F;");
        Label lblTitrePro = new Label(dto.getTitrePro() != null && !dto.getTitrePro().isEmpty()
                ? dto.getTitrePro() : "Candidat");
        lblTitrePro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 500;");
        colCandidat.getChildren().addAll(lblNom, lblTitrePro);

        // Infos offre
        VBox colOffre = new VBox(4);
        colOffre.setPrefWidth(200);
        Label lblOffreTitre = new Label("POSTULÉ POUR");
        lblOffreTitre.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 0 0 1 0;");
        Label lblOffreNom = new Label(dto.getTitreOffre());
        lblOffreNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        colOffre.getChildren().addAll(lblOffreTitre, lblOffreNom);

        // Badge statut traité
        Label lblStatut = new Label("✅ TRAITÉ");
        lblStatut.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;"
                + "-fx-padding: 5 14; -fx-background-radius: 20;"
                + "-fx-font-weight: 700; -fx-font-size: 11px;");

        // Badge entretien
        Label lblEntretien = hasEntretien
                ? new Label("📅 Entretien planifié")
                : new Label("⏳ Sans entretien");
        lblEntretien.setStyle(hasEntretien
                ? "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;"
                : "-fx-background-color:#FEF3C7; -fx-text-fill:#D97706; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Bouton Détails (toujours disponible)
        Button btnDetails = new Button("📄 Détails");
        btnDetails.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569;"
                + "-fx-font-weight: 700; -fx-font-size: 12px;"
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;");
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));

        // Bouton Entretien (si pas encore planifié)
        if (!hasEntretien) {
            Button btnEntretien = new Button("📅 Planifier entretien");
            btnEntretien.setStyle("-fx-background-color: #1E3A5F; -fx-text-fill: white;"
                    + "-fx-font-weight: 700; -fx-font-size: 12px;"
                    + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;"
                    + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);");
            btnEntretien.setOnAction(e -> planifierEntretien(dto, e));
            actions.getChildren().add(btnEntretien);
        }

        actions.getChildren().add(btnDetails);

        row.getChildren().addAll(avatarStack, colCandidat, colOffre, lblStatut, lblEntretien, spacer, actions);
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire d'entretien : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  NAVIGATION SIDEBAR
    // ------------------------------------------------------------------ //
    @FXML
    private void ouvrirCandidatures(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/GestionCandidatures.fxml", "JobNest - Gestion des Candidatures");
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