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
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.service.CandidatureService;
import tn.jobnest.gentretien.service.Entretienservice;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HistoriqueCandidaturesController {

    @FXML private VBox              vboxHistorique;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  comboEntretien;
    @FXML private Label             totalTraitesLabel;
    @FXML private Label             avecEntretienLabel;
    @FXML private Label             sansEntretienLabel;
    @FXML private Label             boostedTraitesLabel;

    private final CandidatureService candidatureService = new CandidatureService();
    private final Entretienservice   entretienService   = new Entretienservice();
    private static final int CURRENT_RECRUTEUR_ID = 1;

    /** Toutes les candidatures traitées */
    private List<CandidatureDTO> toutesTraitees;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        comboEntretien.setItems(FXCollections.observableArrayList("Tous", "Avec entretien", "Sans entretien"));
        comboEntretien.setValue("Tous");
        comboEntretien.valueProperty().addListener((obs, old, nv) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, nv) -> filterAndDisplay());
        chargerDonnees();
    }

    @FXML
    private void rafraichirListe() { chargerDonnees(); }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT — uniquement les candidatures "traité"
    // ─────────────────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        List<CandidatureDTO> toutes = candidatureService.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);
        toutesTraitees = toutes.stream()
                .filter(c -> "traité".equalsIgnoreCase(c.getStatut()) || "traite".equalsIgnoreCase(c.getStatut()))
                .collect(Collectors.toList());
        updateStats();
        filterAndDisplay();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATS
    // ─────────────────────────────────────────────────────────────────────────
    private void updateStats() {
        if (toutesTraitees == null) return;
        long total    = toutesTraitees.size();
        long avecE    = toutesTraitees.stream().filter(c -> aUnEntretienPourOffre(c.getIdCandidat(), c.getIdOffre())).count();
        long sansE    = total - avecE;
        long boosted  = toutesTraitees.stream().filter(CandidatureDTO::isBoosted).count();
        totalTraitesLabel  .setText(String.valueOf(total));
        avecEntretienLabel .setText(String.valueOf(avecE));
        sansEntretienLabel .setText(String.valueOf(sansE));
        boostedTraitesLabel.setText(String.valueOf(boosted));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILTRE + AFFICHAGE
    // ─────────────────────────────────────────────────────────────────────────
    private void filterAndDisplay() {
        vboxHistorique.getChildren().clear();
        if (toutesTraitees == null || toutesTraitees.isEmpty()) {
            afficherMessageVide("Aucune candidature traitée pour le moment."); return;
        }
        String search        = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selEntretien  = comboEntretien.getValue();
        boolean auMoinsUn    = false;

        for (CandidatureDTO dto : toutesTraitees) {
            boolean hasEntretien = aUnEntretienPourOffre(dto.getIdCandidat(), dto.getIdOffre());

            boolean matchSearch = search.isEmpty()
                    || dto.getNomComplet().toLowerCase().contains(search)
                    || dto.getTitreOffre().toLowerCase().contains(search);

            boolean matchEntretien;
            if      ("Avec entretien".equals(selEntretien)) matchEntretien = hasEntretien;
            else if ("Sans entretien".equals(selEntretien)) matchEntretien = !hasEntretien;
            else                                             matchEntretien = true;

            if (matchSearch && matchEntretien) {
                vboxHistorique.getChildren().add(creerItemHistorique(dto, hasEntretien));
                auMoinsUn = true;
            }
        }
        if (!auMoinsUn) afficherMessageVide("Aucun résultat pour ces filtres.");
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

    // ─────────────────────────────────────────────────────────────────────────
    //  VÉRIFICATION : ce candidat a-t-il un entretien pour CETTE offre ?
    // ─────────────────────────────────────────────────────────────────────────
    private boolean aUnEntretienPourOffre(int idCandidat, int idOffre) {
        return candidatureService.candidatADejaUnEntretienPourOffre(idCandidat, idOffre);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRÉATION CARTE HISTORIQUE CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    private HBox creerItemHistorique(CandidatureDTO dto, boolean hasEntretien) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        String couleurBordure = hasEntretien ? "#2563EB" : "#F97316";
        row.setStyle(
                "-fx-padding: 18 22 18 22;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.07), 12, 0, 0, 4);" +
                        "-fx-border-color: " + couleurBordure + ";" +
                        "-fx-border-width: 0 0 0 5;" +
                        "-fx-border-radius: 0 14 14 0;");

        // ── Avatar ────────────────────────────────────────────────────────────
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
        String avatarColor = hasEntretien
                ? "linear-gradient(135deg, #2563EB 0%, #1E40AF 100%)"
                : "linear-gradient(135deg, #F97316 0%, #EA580C 100%)";
        avatarBg.setStyle(
                "-fx-background-color: " + avatarColor + ";" +
                        "-fx-background-radius: 50;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 8, 0, 0, 2);");
        avatarStack.getChildren().addAll(avatarBg, avatarLabel);
        avatarStack.setPrefSize(48, 48);
        avatarStack.setMaxSize(48, 48);
        avatarStack.setMinSize(48, 48);

        if (dto.isBoosted()) {
            Label boostBadge = new Label("⚡");
            boostBadge.setStyle(
                    "-fx-font-size: 10px; -fx-background-color: #7C3AED;" +
                            "-fx-background-radius: 50; -fx-padding: 1 3; -fx-text-fill: white;");
            boostBadge.setTranslateX(16);
            boostBadge.setTranslateY(-16);
            avatarStack.getChildren().add(boostBadge);
        }

        // ── Infos candidat ────────────────────────────────────────────────────
        VBox colCandidat = new VBox(4);
        colCandidat.setPrefWidth(200);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1E3A5F;");
        Label lblTitrePro = new Label(dto.getTitrePro() != null && !dto.getTitrePro().isEmpty()
                ? dto.getTitrePro() : "Candidat");
        lblTitrePro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 500;");
        colCandidat.getChildren().addAll(lblNom, lblTitrePro);

        // ── Infos offre ───────────────────────────────────────────────────────
        VBox colOffre = new VBox(4);
        colOffre.setPrefWidth(200);
        Label lblOffreTitre = new Label("POSTULÉ POUR");
        lblOffreTitre.setStyle(
                "-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 0 0 1 0;");
        Label lblOffreNom = new Label(dto.getTitreOffre());
        lblOffreNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        colOffre.getChildren().addAll(lblOffreTitre, lblOffreNom);

        // ── Badges ────────────────────────────────────────────────────────────
        Label lblStatut = new Label("✅ TRAITÉ");
        lblStatut.setStyle(
                "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;" +
                        "-fx-padding: 5 14; -fx-background-radius: 20;" +
                        "-fx-font-weight: 700; -fx-font-size: 11px;");

        Label lblEntretien = hasEntretien
                ? new Label("📅 Entretien planifié")
                : new Label("⏳ Sans entretien");
        lblEntretien.setStyle(hasEntretien
                ? "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;"
                : "-fx-background-color:#FEF3C7; -fx-text-fill:#D97706; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Actions ───────────────────────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnDetails = new Button("📄 Détails");
        btnDetails.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;" +
                        "-fx-font-weight: 700; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                        "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;");
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));

        if (!hasEntretien) {
            Button btnEntretien = new Button("📅 Planifier entretien");
            btnEntretien.setStyle(
                    "-fx-background-color: #1E3A5F; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);");
            btnEntretien.setOnAction(e -> planifierEntretien(dto));
            actions.getChildren().add(btnEntretien);
        }

        actions.getChildren().add(btnDetails);
        row.getChildren().addAll(avatarStack, colCandidat, colOffre, lblStatut, lblEntretien, spacer, actions);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLANIFIER ENTRETIEN — logique complète
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Logique :
     * 1. Vérifier si ce candidat a déjà un entretien pour cette offre → bloquer si oui
     * 2. Chercher si d'autres entretiens existent pour cette offre (avec d'autres candidats)
     *    → Si oui : proposer de rejoindre un existant OU créer un nouveau
     *    → Si non : créer directement un nouvel entretien
     */
    private void planifierEntretien(CandidatureDTO dto) {
        // Sécurité : vérifier une nouvelle fois que ce candidat n'a pas encore d'entretien pour cette offre
        if (candidatureService.candidatADejaUnEntretienPourOffre(dto.getIdCandidat(), dto.getIdOffre())) {
            showError("Ce candidat a déjà un entretien planifié pour cette offre.");
            return;
        }

        // Chercher les entretiens existants pour cette offre
        List<Entretien> entretiensPourOffre = candidatureService.getEntretiensPourOffre(dto.getIdOffre());

        if (!entretiensPourOffre.isEmpty()) {
            // Des entretiens existent pour cette offre → proposer le choix
            afficherDialogChoixEntretien(dto, entretiensPourOffre);
        } else {
            // Aucun entretien pour cette offre → créer directement
            ouvrirFormulaireNouvelEntretien(dto);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DIALOG : rejoindre un entretien existant OU créer un nouveau
    // ─────────────────────────────────────────────────────────────────────────
    private void afficherDialogChoixEntretien(CandidatureDTO dto, List<Entretien> entretiensPourOffre) {
        // Construire le dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planifier l'entretien");
        dialog.setHeaderText("Des entretiens existent déjà pour cette offre");

        // Contenu du dialog
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #F8FAFC;");

        Label lblIntro = new Label(
                "Des entretiens ont déjà été planifiés pour l'offre « " + dto.getTitreOffre() + " ».\n" +
                        "Souhaitez-vous ajouter « " + dto.getNomComplet() + " » à un entretien existant\n" +
                        "ou créer un nouvel entretien indépendant ?"
        );
        lblIntro.setWrapText(true);
        lblIntro.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E3A5F;");

        // Liste des entretiens existants (radio buttons)
        Label lblChoix = new Label("Entretiens existants pour cette offre :");
        lblChoix.setStyle("-fx-font-weight: 700; -fx-text-fill: #1E3A5F; -fx-font-size: 12px;");

        ToggleGroup group = new ToggleGroup();
        VBox radioBox = new VBox(8);
        radioBox.setPadding(new Insets(0, 0, 0, 10));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Entretien e : entretiensPourOffre) {
            String dateStr = e.getDateEntretien() != null
                    ? e.getDateEntretien().toLocalDate().format(fmt) : "Date inconnue";
            String heureStr = e.getHeureDebut() != null
                    ? e.getHeureDebut().toLocalTime().toString() : "--:--";
            String type = e.getTypeEntretien() != null ? e.getTypeEntretien() : "";
            String lieu = "présentiel".equals(type)
                    ? (e.getLieu() != null && !e.getLieu().isBlank() ? e.getLieu() : "Lieu non défini")
                    : (e.getLienVisio() != null && !e.getLienVisio().isBlank() ? "Visio (Meet)" : "Lien non défini");

            RadioButton rb = new RadioButton(
                    "📅 " + dateStr + " à " + heureStr +
                            "  •  " + type + "  •  " + lieu +
                            "  [" + (e.getStatut() != null ? e.getStatut() : "?") + "]"
            );
            rb.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            rb.setToggleGroup(group);
            rb.setUserData(e); // stocker l'objet Entretien dans le RadioButton
            radioBox.getChildren().add(rb);
        }

        content.getChildren().addAll(lblIntro, lblChoix, radioBox);

        // Séparateur et option "Nouveau"
        Separator sep = new Separator();
        Label lblNouv = new Label("— OU —");
        lblNouv.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-alignment: center;");
        content.getChildren().addAll(sep, lblNouv);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(600);

        // Boutons
        ButtonType btnRejoindre   = new ButtonType("Rejoindre l'entretien sélectionné", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNouvel      = new ButtonType("Créer un nouvel entretien",          ButtonBar.ButtonData.OTHER);
        ButtonType btnAnnuler     = new ButtonType("Annuler",                            ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRejoindre, btnNouvel, btnAnnuler);

        // Activer "Rejoindre" seulement si un entretien est sélectionné
        Node nodeRejoindre = dialog.getDialogPane().lookupButton(btnRejoindre);
        nodeRejoindre.setDisable(true);
        group.selectedToggleProperty().addListener((obs, old, nv) ->
                nodeRejoindre.setDisable(nv == null));

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnRejoindre) {
                // L'utilisateur veut rejoindre un entretien existant
                Toggle selected = group.getSelectedToggle();
                if (selected != null) {
                    Entretien entretienChoisi = (Entretien) selected.getUserData();
                    rejoindreEntretienExistant(dto, entretienChoisi);
                }
            } else if (result.get() == btnNouvel) {
                // Créer un nouvel entretien indépendant
                ouvrirFormulaireNouvelEntretien(dto);
            }
            // btnAnnuler → ne rien faire
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REJOINDRE UN ENTRETIEN EXISTANT
    // ─────────────────────────────────────────────────────────────────────────
    private void rejoindreEntretienExistant(CandidatureDTO dto, Entretien entretien) {
        boolean ok = candidatureService.ajouterParticipant(entretien.getIdEntretien(), dto.getIdCandidat());
        if (ok) {
            showInfo("Succès",
                    "« " + dto.getNomComplet() + " » a été ajouté à l'entretien du "
                            + (entretien.getDateEntretien() != null
                            ? entretien.getDateEntretien().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "?")
                            + ".\nIl apparaîtra désormais dans les participants de cet entretien.");
            chargerDonnees(); // rafraîchir l'affichage
        } else {
            showError("Impossible d'ajouter le participant. Vérifiez la base de données.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUVRIR LE FORMULAIRE DE NOUVEL ENTRETIEN
    // ─────────────────────────────────────────────────────────────────────────
    private void ouvrirFormulaireNouvelEntretien(CandidatureDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Parent root = loader.load();
            EntretienFormController ctrl = loader.getController();

            // Injecter le contexte dynamique : candidat + offre
            ctrl.setContextCandidature(
                    dto.getIdCandidat(),
                    dto.getIdOffre(),
                    dto.getNomComplet(),
                    dto.getTitreOffre()
            );

            Stage stage = new Stage();
            stage.setTitle("Planifier un entretien — " + dto.getNomComplet() + " / " + dto.getTitreOffre());
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            // Quand la fenêtre se ferme, rafraîchir la liste
            stage.setOnHidden(ev -> chargerDonnees());
            stage.show();

        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire d'entretien : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUVRIR DÉTAILS CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    private void ouvrirDetails(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/candidature-details.fxml"));
            Parent root = loader.load();
            CandidatureDetailsController controller = loader.getController();
            controller.chargerDonnees(dto);
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
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
    @FXML private void ouvrirCandidatures(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/GestionCandidatures.fxml", "JobNest - Gestion des Candidatures");
    }
    @FXML private void ouvrirEntretiens(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/entretien-view.fxml", "JobNest - Gestion des Entretiens");
    }
    @FXML private void ouvrirFeedbacks(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/feedback-interface.fxml", "JobNest - Gestion des Feedbacks");
    }

    private void naviguer(ActionEvent event, String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage  = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene  = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("JobNest");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}