package tn.jobnest.gentretien.controller;

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
    @FXML private Label totalCandidaturesLabel, boostedLabel, enAttenteLabel;
    @FXML private TextField searchField;

    private final CandidatureService service = new CandidatureService();
    private final int CURRENT_RECRUTEUR_ID = 1; // statique pour le moment
    private List<CandidatureDTO> toutesLesCandidatures;

    @FXML
    public void initialize() {
        chargerDonnees();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerCandidatures(newVal));
        }
    }

    private void chargerDonnees() {
        toutesLesCandidatures = service.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);
        mettreAJourStats(toutesLesCandidatures);
        afficherCandidatures(toutesLesCandidatures);
    }

    @FXML
    private void rafraichirListe() {
        chargerDonnees();
    }

    private void mettreAJourStats(List<CandidatureDTO> liste) {
        totalCandidaturesLabel.setText(String.valueOf(liste.size()));
        boostedLabel.setText(String.valueOf(
                liste.stream().filter(CandidatureDTO::isBoosted).count()));
        enAttenteLabel.setText(String.valueOf(
                liste.stream().filter(c -> estEnAttente(c.getStatut())).count()));
    }

    private void filtrerCandidatures(String query) {
        if (query == null || query.isEmpty()) {
            afficherCandidatures(toutesLesCandidatures);
            return;
        }
        String q = query.toLowerCase();
        List<CandidatureDTO> filtrees = toutesLesCandidatures.stream()
                .filter(c -> c.getNomComplet().toLowerCase().contains(q) ||
                        c.getTitreOffre().toLowerCase().contains(q))
                .collect(Collectors.toList());
        afficherCandidatures(filtrees);
    }

    private void afficherCandidatures(List<CandidatureDTO> liste) {
        vboxCandidatures.getChildren().clear();
        if (liste.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label emptyIcon = new Label("📭");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyText = new Label("Aucune candidature trouvée");
            emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
            Label emptySubText = new Label("Les candidatures s'afficheront ici une fois reçues");
            emptySubText.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1;");
            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptySubText);
            vboxCandidatures.getChildren().add(emptyBox);
        } else {
            for (CandidatureDTO dto : liste) {
                vboxCandidatures.getChildren().add(creerItemCandidature(dto));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers statut
    // ────────────────────────────────────────────────────────────────────
    private boolean estEnAttente(String statut) {
        return statut != null &&
                (statut.equalsIgnoreCase("en attente") || statut.equalsIgnoreCase("en_attente"));
    }

    private boolean estAcceptee(String statut) {
        return statut != null &&
                (statut.equalsIgnoreCase("traité") || statut.equalsIgnoreCase("traite"));
    }

    // ────────────────────────────────────────────────────────────────────
    // Construction de la carte candidature
    // ────────────────────────────────────────────────────────────────────
    private HBox creerItemCandidature(CandidatureDTO dto) {
        String statut = dto.getStatut() != null ? dto.getStatut() : "inconnu";

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 18 22 18 22;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.07), 12, 0, 0, 4);" +
                        (dto.isBoosted()
                                ? "-fx-border-color: #7C3AED; -fx-border-width: 0 0 0 5; -fx-border-radius: 0 14 14 0;"
                                : "-fx-border-color: #EFF3FB; -fx-border-width: 1; -fx-border-radius: 14;")
        );

        // ── Avatar ──
        String nomComplet = dto.getNomComplet();
        String[] parts = nomComplet.split(" ");
        String initials = (parts.length >= 2)
                ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                : nomComplet.substring(0, Math.min(2, nomComplet.length())).toUpperCase();

        StackPane avatarStack = new StackPane();
        Region avatarBg = new Region();
        avatarBg.setPrefSize(48, 48);
        avatarBg.setStyle(
                "-fx-background-color: linear-gradient(135deg, #2563EB 0%, #1E40AF 100%);" +
                        "-fx-background-radius: 50;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 8, 0, 0, 2);"
        );
        Label avatarLabel = new Label(initials);
        avatarLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
        avatarStack.getChildren().addAll(avatarBg, avatarLabel);
        avatarStack.setPrefSize(48, 48);
        avatarStack.setMaxSize(48, 48);
        avatarStack.setMinSize(48, 48);

        if (dto.isBoosted()) {
            Label boostBadge = new Label("⚡");
            boostBadge.setStyle(
                    "-fx-font-size: 10px; -fx-background-color: #7C3AED;" +
                            "-fx-background-radius: 50; -fx-padding: 1 3; -fx-text-fill: white;"
            );
            boostBadge.setTranslateX(16);
            boostBadge.setTranslateY(-16);
            avatarStack.getChildren().add(boostBadge);
        }

        // ── Infos candidat ──
        VBox colCandidat = new VBox(4);
        colCandidat.setPrefWidth(210);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1E3A5F;");
        Label lblTitrePro = new Label(
                dto.getTitrePro() != null && !dto.getTitrePro().isEmpty() ? dto.getTitrePro() : "Candidat");
        lblTitrePro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 500;");
        colCandidat.getChildren().addAll(lblNom, lblTitrePro);

        // ── Infos offre ──
        VBox colOffre = new VBox(4);
        colOffre.setPrefWidth(210);
        Label lblOffreTitre = new Label("POSTULÉ POUR");
        lblOffreTitre.setStyle(
                "-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 0 0 1 0;"
        );
        Label lblOffreNom = new Label(dto.getTitreOffre());
        lblOffreNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        colOffre.getChildren().addAll(lblOffreTitre, lblOffreNom);

        // ── Badge statut ──
        String bgColor, textColor, emoji;
        if (estAcceptee(statut)) {
            bgColor = "#DCFCE7"; textColor = "#15803D"; emoji = "✅ ";
        } else if (statut.equalsIgnoreCase("refusé") || statut.equalsIgnoreCase("refuse")) {
            bgColor = "#FEE2E2"; textColor = "#DC2626"; emoji = "❌ ";
        } else {
            bgColor = "#FEF3C7"; textColor = "#D97706"; emoji = "⏳ ";
        }
        Label lblStatut = new Label(emoji + statut.toUpperCase());
        lblStatut.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + ";" +
                        "-fx-padding: 5 14; -fx-background-radius: 20; -fx-font-weight: 700; -fx-font-size: 11px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Zone boutons ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // ── Bouton ACCEPTER : visible uniquement si en attente ──
        if (estEnAttente(statut)) {
            Button btnAccepter = new Button("✅ Accepter");
            btnAccepter.setStyle(
                    "-fx-background-color: #16A34A; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.3), 8, 0, 0, 2);"
            );
            btnAccepter.setOnAction(e -> {
                if (service.modifierStatut(dto.getIdCandidature(), "Traité")) {
                    rafraichirListe(); // Rafraîchit → bouton Entretien apparaît
                }
            });
            actions.getChildren().add(btnAccepter);
        }

        // ── Bouton DÉTAILS : toujours visible ──
        Button btnDetails = new Button("📄 Détails");
        btnDetails.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;" +
                        "-fx-font-weight: 700; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                        "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;"
        );
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));
        actions.getChildren().add(btnDetails);

        // ── Bouton ANNULER : uniquement si en_attente ──
        // Si la candidature est "Traité" (acceptée), on ne peut plus l'annuler.
        // La candidature passe à "annulé" en BD et disparaît de l'interface.
        if (estEnAttente(statut)) {
            Button btnAnnuler = new Button("❌ Annuler");
            btnAnnuler.setStyle(
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-border-color: #FECACA; -fx-border-radius: 8; -fx-border-width: 1;"
            );
            Tooltip tipAnnuler = new Tooltip(
                    "Annuler cette candidature\n" +
                            "Elle sera masquée de la liste mais conservée en base de données.\n" +
                            "⚠️ Impossible d'annuler une candidature déjà acceptée."
            );
            tipAnnuler.setStyle("-fx-font-size: 12px;");
            Tooltip.install(btnAnnuler, tipAnnuler);
            btnAnnuler.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmer l'annulation");
                confirm.setHeaderText("Annuler la candidature de " + dto.getNomComplet() + " ?");
                confirm.setContentText(
                        "Cette candidature sera marquée comme annulée.\n" +
                                "Elle disparaîtra de cette liste mais restera en base de données."
                );
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        if (service.annulerCandidature(dto.getIdCandidature())) {
                            rafraichirListe();
                        }
                    }
                });
            });
            actions.getChildren().add(btnAnnuler);
        }

        // ── Bouton ENTRETIEN : visible UNIQUEMENT si candidature acceptée (Traité) ──
        // Désactivé si un entretien existe déjà pour ce candidat + cette offre.
        if (estAcceptee(statut)) {
            int idOffre = service.getIdOffreByCandidature(dto.getIdCandidature());
            boolean dejaEntretien = service.entretienDejaExiste(dto.getIdCandidat(), idOffre);

            Button btnEntretien = new Button(dejaEntretien ? "📅 Entretien planifié" : "📅 Planifier Entretien");
            btnEntretien.setStyle(
                    dejaEntretien
                            ? "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: default;"
                            : "-fx-background-color: #1E3A5F; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);"
            );

            if (dejaEntretien) {
                btnEntretien.setDisable(true);
                Tooltip tip = new Tooltip("🔒 Un entretien a déjà été planifié pour cette candidature.\nUne candidature ne peut avoir qu'un seul entretien.");
                tip.setStyle("-fx-font-size: 12px;");
                Tooltip.install(btnEntretien, tip);
            } else {
                Tooltip tip = new Tooltip(
                        "Planifier un entretien pour " + dto.getNomComplet() +
                                "\nLe candidat sera automatiquement ajouté comme participant."
                );
                tip.setStyle("-fx-font-size: 12px;");
                Tooltip.install(btnEntretien, tip);
                btnEntretien.setOnAction(e -> planifierEntretien(dto));
            }
            actions.getChildren().add(btnEntretien);
        }

        row.getChildren().addAll(avatarStack, colCandidat, colOffre, lblStatut, spacer, actions);
        return row;
    }

    // ────────────────────────────────────────────────────────────────────
    // Actions
    // ────────────────────────────────────────────────────────────────────

    private void ouvrirDetails(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/jobnest/gentretien/candidature-details.fxml"));
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

    /**
     * Ouvre le formulaire d'entretien en mode "candidature acceptée".
     * Transmet l'id_candidat et l'id_offre au formulaire.
     * Après création de l'entretien, le candidat est inséré dans participant_entretien.
     */
    private void planifierEntretien(CandidatureDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/tn/jobnest/gentretien/entretien-form.fxml"));
            Parent root = loader.load();

            EntretienFormController ctrl = loader.getController();

            // Récupérer l'id_offre de cette candidature
            int idOffre = service.getIdOffreByCandidature(dto.getIdCandidature());

            // Injecter le contexte : candidat + offre
            ctrl.setContexteCandidature(dto.getIdCandidat(), idOffre);

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.setTitle("📅 Planifier un entretien — " + dto.getNomComplet());
            stage.showAndWait(); // Attendre la fermeture avant de rafraîchir si besoin
        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire d'entretien : " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("JobNest");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Navigation sidebar ──────────────────────────────────────────────

    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/entretien-view.fxml");
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/feedback-interface.fxml");
    }

    private void naviguer(ActionEvent event, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }
}