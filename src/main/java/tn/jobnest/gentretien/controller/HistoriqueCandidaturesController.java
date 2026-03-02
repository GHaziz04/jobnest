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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private void chargerDonnees() {
        List<CandidatureDTO> toutes = candidatureService.getCandidaturesPourRecruteur(CURRENT_RECRUTEUR_ID);
        toutesTraitees = toutes.stream()
                .filter(c -> "traité".equalsIgnoreCase(c.getStatut()) || "traite".equalsIgnoreCase(c.getStatut()))
                .collect(Collectors.toList());
        updateStats();
        filterAndDisplay();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void updateStats() {
        if (toutesTraitees == null) return;
        long total   = toutesTraitees.size();
        long avecE   = toutesTraitees.stream().filter(c -> aUnEntretienPourOffre(c.getIdCandidat(), c.getIdOffre())).count();
        long sansE   = total - avecE;
        long boosted = toutesTraitees.stream().filter(CandidatureDTO::isBoosted).count();
        totalTraitesLabel  .setText(String.valueOf(total));
        avecEntretienLabel .setText(String.valueOf(avecE));
        sansEntretienLabel .setText(String.valueOf(sansE));
        boostedTraitesLabel.setText(String.valueOf(boosted));
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void filterAndDisplay() {
        vboxHistorique.getChildren().clear();
        if (toutesTraitees == null || toutesTraitees.isEmpty()) {
            afficherMessageVide("Aucune candidature traitée pour le moment.");
            return;
        }

        String  search       = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String  selEntretien = comboEntretien.getValue();
        boolean auMoinsUn   = false;

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
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 48px;");
        Label text = new Label(message);
        text.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
        box.getChildren().addAll(icon, text);
        vboxHistorique.getChildren().add(box);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private boolean aUnEntretienPourOffre(int idCandidat, int idOffre) {
        return candidatureService.candidatADejaUnEntretienPourOffre(idCandidat, idOffre);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CARTE CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    private HBox creerItemHistorique(CandidatureDTO dto, boolean hasEntretien) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 18 22 18 22;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.07), 12, 0, 0, 4);" +
                        "-fx-border-color: " + (hasEntretien ? "#2563EB" : "#F97316") + ";" +
                        "-fx-border-width: 0 0 0 5;" +
                        "-fx-border-radius: 0 14 14 0;"
        );

        // ── Avatar ─────────────────────────────────────────────────────────
        String nom      = dto.getNomComplet();
        String[] parts  = nom.split(" ");
        String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))
                : nom.substring(0, Math.min(2, nom.length()));
        initials = initials.toUpperCase();

        StackPane avatarStack = new StackPane();
        Region avatarBg = new Region();
        avatarBg.setPrefSize(48, 48);
        // ✅ Gradient JavaFX valide (from/to, pas de degrés)
        avatarBg.setStyle(
                "-fx-background-color: " + (hasEntretien
                        ? "linear-gradient(from 0% 0% to 100% 100%, #2563EB, #1E40AF)"
                        : "linear-gradient(from 0% 0% to 100% 100%, #F97316, #EA580C)") + ";" +
                        "-fx-background-radius: 50;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 6, 0, 0, 2);"
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
                    "-fx-font-size: 9px; -fx-background-color: #7C3AED;" +
                            "-fx-background-radius: 50; -fx-padding: 1 3; -fx-text-fill: white;"
            );
            boostBadge.setTranslateX(16);
            boostBadge.setTranslateY(-16);
            avatarStack.getChildren().add(boostBadge);
        }

        // ── Candidat ───────────────────────────────────────────────────────
        VBox colCandidat = new VBox(4);
        colCandidat.setPrefWidth(200);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1E3A5F;");
        Label lblPro = new Label(dto.getTitrePro() != null && !dto.getTitrePro().isEmpty()
                ? dto.getTitrePro() : "Candidat");
        lblPro.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        colCandidat.getChildren().addAll(lblNom, lblPro);

        // ── Offre ──────────────────────────────────────────────────────────
        VBox colOffre = new VBox(4);
        colOffre.setPrefWidth(200);
        Label offreKey = new Label("POSTULÉ POUR");
        offreKey.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: 700;");
        Label offreVal = new Label(dto.getTitreOffre());
        offreVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #2563EB; -fx-font-size: 13px;");
        colOffre.getChildren().addAll(offreKey, offreVal);

        // ── Badges ─────────────────────────────────────────────────────────
        Label lblStatut = new Label("✅ TRAITÉ");
        lblStatut.setStyle(
                "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;" +
                        "-fx-padding: 5 14; -fx-background-radius: 20;" +
                        "-fx-font-weight: 700; -fx-font-size: 11px;"
        );

        Label lblEntretien = hasEntretien
                ? new Label("📅 Entretien planifié")
                : new Label("⏳ Sans entretien");
        lblEntretien.setStyle(hasEntretien
                ? "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;"
                : "-fx-background-color:#FEF3C7; -fx-text-fill:#D97706; -fx-padding:5 14; -fx-background-radius:20; -fx-font-weight:700; -fx-font-size:11px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Actions ────────────────────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnDetails = new Button("📄 Détails");
        btnDetails.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;" +
                        "-fx-font-weight: 700; -fx-font-size: 12px;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                        "-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1;"
        );
        btnDetails.setOnAction(e -> ouvrirDetails(dto, e));

        if (!hasEntretien) {
            Button btnEntretien = new Button("📅 Planifier");
            btnEntretien.setStyle(
                    "-fx-background-color: #1E3A5F; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);"
            );
            btnEntretien.setOnMouseEntered(e -> btnEntretien.setStyle(
                    "-fx-background-color: #2563EB; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 10, 0, 0, 3);"
            ));
            btnEntretien.setOnMouseExited(e -> btnEntretien.setStyle(
                    "-fx-background-color: #1E3A5F; -fx-text-fill: white;" +
                            "-fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.3), 8, 0, 0, 2);"
            ));
            btnEntretien.setOnAction(e -> planifierEntretien(dto));
            actions.getChildren().add(btnEntretien);
        }

        actions.getChildren().add(btnDetails);
        row.getChildren().addAll(avatarStack, colCandidat, colOffre, lblStatut, lblEntretien, spacer, actions);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLANIFIER ENTRETIEN
    //  ✅ LOGIQUE FILTRE DATE :
    //     On ne garde que les entretiens dont la date est >= aujourd'hui.
    //     Si la liste filtrée est vide → on crée directement sans dialog.
    // ─────────────────────────────────────────────────────────────────────────
    private void planifierEntretien(CandidatureDTO dto) {
        // Sécurité : vérifier qu'il n'a pas déjà un entretien
        if (candidatureService.candidatADejaUnEntretienPourOffre(dto.getIdCandidat(), dto.getIdOffre())) {
            showError("Ce candidat a déjà un entretien planifié pour cette offre.");
            return;
        }

        // Récupérer tous les entretiens existants pour cette offre
        List<Entretien> tousEntretiensPourOffre = candidatureService.getEntretiensPourOffre(dto.getIdOffre());

        // ✅ FILTRE : garder uniquement les entretiens :
        //    - dont la date n'est PAS dépassée (date_entretien >= aujourd'hui)
        //    - ET dont le statut n'est PAS "annulé"
        LocalDate today = LocalDate.now();
        List<Entretien> entretiensValides = tousEntretiensPourOffre.stream()
                .filter(e -> {
                    // Exclure si date dépassée
                    if (e.getDateEntretien() == null) return false;
                    if (e.getDateEntretien().toLocalDate().isBefore(today)) return false;
                    // Exclure si statut annulé
                    if ("annulé".equalsIgnoreCase(e.getStatut())) return false;
                    return true;
                })
                .collect(Collectors.toList());

        // Récupérer la fenêtre courante
        Stage ownerStage = null;
        if (vboxHistorique.getScene() != null && vboxHistorique.getScene().getWindow() instanceof Stage)
            ownerStage = (Stage) vboxHistorique.getScene().getWindow();

        if (entretiensValides.isEmpty()) {
            // ✅ Tous les entretiens sont passés (ou aucun) → créer directement, sans dialog
            ouvrirFormulaireNouvelEntretien(dto);
        } else {
            // Des entretiens valides (non dépassés) existent → proposer le choix via dialog
            PlanifierEntretienDialog.DialogResult result = PlanifierEntretienDialog.show(
                    entretiensValides,          // ← on passe UNIQUEMENT les entretiens valides
                    dto.getNomComplet(),
                    dto.getTitreOffre(),
                    ownerStage
            );

            switch (result.action) {
                case REJOINDRE_EXISTANT:
                    rejoindreEntretienExistant(dto, result.entretienChoisi);
                    break;
                case CREER_NOUVEAU:
                    ouvrirFormulaireNouvelEntretien(dto);
                    break;
                case ANNULER:
                default:
                    break;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void rejoindreEntretienExistant(CandidatureDTO dto, Entretien entretien) {
        boolean ok = candidatureService.ajouterParticipant(entretien.getIdEntretien(), dto.getIdCandidat());
        if (ok) {
            showInfo("Participant ajouté",
                    "« " + dto.getNomComplet() + " » a été ajouté à l'entretien du "
                            + (entretien.getDateEntretien() != null
                            ? entretien.getDateEntretien().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "?") + "."
            );
            chargerDonnees();
        } else {
            showError("Impossible d'ajouter le participant. Vérifiez la base de données.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void ouvrirFormulaireNouvelEntretien(CandidatureDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Parent root = loader.load();
            EntretienFormController ctrl = loader.getController();
            ctrl.setContextCandidature(
                    dto.getIdCandidat(), dto.getIdOffre(),
                    dto.getNomComplet(), dto.getTitreOffre()
            );

            Stage stage = new Stage();
            stage.setTitle("Planifier un entretien — " + dto.getNomComplet() + " / " + dto.getTitreOffre());
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null)
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(ev -> chargerDonnees());
            stage.show();

        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void ouvrirDetails(CandidatureDTO dto, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/candidature-details.fxml"));
            Parent root = loader.load();
            CandidatureDetailsController controller = loader.getController();
            controller.chargerDonnees(dto);
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Documents de " + dto.getNomComplet());
            s.setScene(new Scene(root));
            s.showAndWait();
        } catch (IOException e) {
            showError("Impossible d'ouvrir les détails : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION SIDEBAR
    // ─────────────────────────────────────────────────────────────────────────
    @FXML private void ouvrirCandidatures(ActionEvent e) {
        naviguer(e, "/tn/jobnest/gentretien/GestionCandidatures.fxml", "JobNest - Candidatures");
    }
    @FXML private void ouvrirEntretiens(ActionEvent e) {
        naviguer(e, "/tn/jobnest/gentretien/entretien-view.fxml", "JobNest - Entretiens");
    }
    @FXML private void ouvrirFeedbacks(ActionEvent e) {
        naviguer(e, "/tn/jobnest/gentretien/feedback-interface.fxml", "JobNest - Feedbacks");
    }

    private void naviguer(ActionEvent event, String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene  scene = new Scene(root);
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
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("JobNest"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }



}