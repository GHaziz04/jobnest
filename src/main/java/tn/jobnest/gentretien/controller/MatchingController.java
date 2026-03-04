package tn.jobnest.gentretien.controller;

import javafx.application.Platform;
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
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.service.CandidatureService;
import tn.jobnest.gentretien.service.MatchingService;
import tn.jobnest.gentretien.service.MatchingService.MatchingResult;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ════════════════════════════════════════════════════════════════
 *  MatchingController — Interface de matching candidatures/offres
 * ════════════════════════════════════════════════════════════════
 *
 *  Affiche les candidatures en attente avec leur score de matching.
 *  Permet au recruteur de lancer le matching automatique ou manuel.
 */
public class MatchingController {

    // ─── FXML ────────────────────────────────────────────────────
    @FXML private VBox   vboxCandidatures;
    @FXML private Label  totalLabel;
    @FXML private Label  enAttenteLabel;
    @FXML private Label  acceptesLabel;
    @FXML private Label  revisionLabel;
    @FXML private Label  annulesLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private Button btnMatchingTout;

    // ─── Services ────────────────────────────────────────────────
    private final CandidatureService candidatureService = new CandidatureService();
    private final MatchingService    matchingService    = new MatchingService();
    private static final int RECRUTEUR_ID = 1;

    private List<CandidatureDTO> toutesLesCandidatures;

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        comboFiltre.setItems(FXCollections.observableArrayList(
                "Toutes", "En attente", "En révision", "Acceptées", "Annulées"));
        comboFiltre.setValue("Toutes");
        comboFiltre.valueProperty().addListener((obs, o, n) -> filtrerEtAfficher());

        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> filtrerEtAfficher());

        chargerDonnees();
    }

    // ════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ════════════════════════════════════════════════════════════
    private void chargerDonnees() {
        toutesLesCandidatures = candidatureService.getCandidaturesPourRecruteur(RECRUTEUR_ID);
        mettreAJourStats();
        filtrerEtAfficher();
    }

    @FXML
    private void rafraichir() { chargerDonnees(); }

    // ════════════════════════════════════════════════════════════
    //  MATCHING DE TOUTES LES CANDIDATURES EN ATTENTE
    // ════════════════════════════════════════════════════════════
    @FXML
    private void lancerMatchingTout() {
        List<CandidatureDTO> enAttente = toutesLesCandidatures.stream()
                .filter(c -> "en_attente".equalsIgnoreCase(c.getStatut())
                        || "en attente".equalsIgnoreCase(c.getStatut()))
                .collect(Collectors.toList());

        if (enAttente.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aucune candidature en attente à analyser.").showAndWait();
            return;
        }

        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Lancer le Matching");
        confirm.setHeaderText("Analyser " + enAttente.size() + " candidature(s) en attente ?");
        confirm.setContentText(
                "Le système va calculer automatiquement le score de matching " +
                        "pour chaque candidature et appliquer les décisions suivantes :\n\n" +
                        "  • Score ≥ 80% → Acceptée automatiquement\n" +
                        "  • Score 40–79% → Mise en révision (vous décidez)\n" +
                        "  • Score < 40% → Annulée automatiquement");

        ButtonType btnLancer  = new ButtonType("▶  Lancer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnLancer, btnAnnuler);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnLancer) {
                // Lancer en arrière-plan pour ne pas bloquer l'UI
                btnMatchingTout.setDisable(true);
                btnMatchingTout.setText("⏳ Analyse en cours...");

                Thread thread = new Thread(() -> {
                    int acceptes = 0, revisions = 0, annules = 0;
                    for (CandidatureDTO dto : enAttente) {
                        MatchingResult result = matchingService.calculerEtAppliquer(
                                dto.getIdCandidature(), dto.getIdOffre(), dto.getIdCandidat());
                        System.out.println(result.toString());
                        if (result.estAcceptee())   acceptes++;
                        else if (result.estEnRevision()) revisions++;
                        else                          annules++;
                    }
                    final int fa = acceptes, fr = revisions, fn = annules;
                    Platform.runLater(() -> {
                        btnMatchingTout.setDisable(false);
                        btnMatchingTout.setText("🎯  Lancer le Matching");
                        chargerDonnees();
                        afficherResultatGlobal(fa, fr, fn);
                    });
                });
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private void afficherResultatGlobal(int acceptes, int revisions, int annules) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Matching terminé !");
        info.setHeaderText("✅  Analyse terminée");
        info.setContentText(String.format(
                "Résultats du matching automatique :\n\n" +
                        "  ✅  Acceptées automatiquement : %d\n" +
                        "  🔍  Mises en révision          : %d\n" +
                        "  ❌  Annulées automatiquement   : %d",
                acceptes, revisions, annules));
        info.showAndWait();
    }

    // ════════════════════════════════════════════════════════════
    //  FILTRES + AFFICHAGE
    // ════════════════════════════════════════════════════════════
    private void filtrerEtAfficher() {
        if (toutesLesCandidatures == null) return;

        String filtre = comboFiltre.getValue();
        String search = searchField != null ? searchField.getText().toLowerCase() : "";

        List<CandidatureDTO> filtrees = toutesLesCandidatures.stream()
                .filter(c -> {
                    switch (filtre == null ? "Toutes" : filtre) {
                        case "En attente":  return "en_attente".equalsIgnoreCase(c.getStatut());
                        case "En révision": return "en_revision".equalsIgnoreCase(c.getStatut());
                        case "Acceptées":   return "traité".equalsIgnoreCase(c.getStatut());
                        case "Annulées":    return "annulé".equalsIgnoreCase(c.getStatut());
                        default:            return true;
                    }
                })
                .filter(c -> search.isEmpty()
                        || c.getNomComplet().toLowerCase().contains(search)
                        || c.getTitreOffre().toLowerCase().contains(search))
                .collect(Collectors.toList());

        afficherCandidatures(filtrees);
    }

    private void afficherCandidatures(List<CandidatureDTO> liste) {
        vboxCandidatures.getChildren().clear();
        if (liste.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("🎯"); icon.setStyle("-fx-font-size:48px;");
            Label msg  = new Label("Aucune candidature pour ce filtre");
            msg.setStyle("-fx-font-size:16px; -fx-text-fill:#94A3B8; -fx-font-weight:700;");
            empty.getChildren().addAll(icon, msg);
            vboxCandidatures.getChildren().add(empty);
            return;
        }
        for (CandidatureDTO dto : liste) {
            vboxCandidatures.getChildren().add(creerCarteCandidat(dto));
        }
    }

    // ════════════════════════════════════════════════════════════
    //  CARTE CANDIDAT avec score et boutons
    // ════════════════════════════════════════════════════════════
    private VBox creerCarteCandidat(CandidatureDTO dto) {
        boolean enAttente   = "en_attente".equalsIgnoreCase(dto.getStatut());
        boolean enRevision  = "en_revision".equalsIgnoreCase(dto.getStatut());
        boolean accepte     = "traité".equalsIgnoreCase(dto.getStatut());
        boolean annule      = "annulé".equalsIgnoreCase(dto.getStatut());

        VBox card = new VBox(14);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.09), 14, 0, 0, 5);" +
                        bordureCouleur(dto.getStatut()));

        // ─── Ligne du haut ───────────────────────────────────────
        HBox topRow = new HBox(18);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane avatar = creerAvatar(dto.getNomComplet(), accepte);

        // Infos
        VBox infos = new VBox(5);
        HBox.setHgrow(infos, Priority.ALWAYS);
        Label lblNom = new Label(dto.getNomComplet());
        lblNom.setStyle("-fx-font-weight:800; -fx-font-size:17px; -fx-text-fill:#1E3A5F;");
        Label lblOffre = new Label("📋 Postulé pour : " + dto.getTitreOffre());
        lblOffre.setStyle("-fx-font-size:13px; -fx-text-fill:#2563EB; -fx-font-weight:600;");
        infos.getChildren().addAll(lblNom, lblOffre);

        // Badge statut
        Label badge = creerBadgeStatut(dto.getStatut());

        topRow.getChildren().addAll(avatar, infos, badge);

        // ─── Séparateur ──────────────────────────────────────────
        Separator sep = new Separator();

        // ─── Ligne d'actions ─────────────────────────────────────
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (enAttente) {
            // Seul bouton disponible : lancer le matching individuel
            Button btnMatch = new Button("🎯  Analyser (Matching)");
            btnMatch.setPrefHeight(44);
            btnMatch.setStyle(
                    "-fx-background-color: linear-gradient(to right, #7C3AED, #5B21B6);" +
                            "-fx-text-fill: white; -fx-font-weight:800; -fx-font-size:13px;" +
                            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:0 20;");
            btnMatch.setOnAction(e -> lancerMatchingIndividuel(dto, card));
            actionsRow.getChildren().add(btnMatch);

        } else if (enRevision) {
            // Le recruteur doit décider : accepter ou rejeter
            Label conseil = new Label("⚠️  Score modéré — votre décision est requise");
            conseil.setStyle("-fx-font-size:12px; -fx-text-fill:#D97706; -fx-font-weight:600;");
            HBox.setHgrow(conseil, Priority.ALWAYS);

            Button btnAccepter = new Button("✅  Accepter");
            btnAccepter.setPrefHeight(40);
            btnAccepter.setStyle(
                    "-fx-background-color:#059669; -fx-text-fill:white;" +
                            "-fx-font-weight:800; -fx-font-size:13px;" +
                            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:0 18;");
            btnAccepter.setOnAction(e -> changerStatut(dto, "traité", card));

            Button btnRejeter = new Button("❌  Rejeter");
            btnRejeter.setPrefHeight(40);
            btnRejeter.setStyle(
                    "-fx-background-color:#DC2626; -fx-text-fill:white;" +
                            "-fx-font-weight:800; -fx-font-size:13px;" +
                            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:0 18;");
            btnRejeter.setOnAction(e -> changerStatut(dto, "annulé", card));

            Button btnDetail = new Button("📊  Voir Détail");
            btnDetail.setPrefHeight(40);
            btnDetail.setStyle(
                    "-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8;" +
                            "-fx-font-weight:700; -fx-font-size:13px;" +
                            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:0 14;");
            btnDetail.setOnAction(e -> afficherDetailMatching(dto));

            actionsRow.getChildren().addAll(conseil, btnDetail, btnAccepter, btnRejeter);

        } else {
            // Candidature déjà traitée
            Button btnDetail = new Button("📊  Voir Détail Matching");
            btnDetail.setPrefHeight(40);
            btnDetail.setStyle(
                    "-fx-background-color:#F1F5F9; -fx-text-fill:#475569;" +
                            "-fx-font-weight:700; -fx-font-size:13px;" +
                            "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:0 14;" +
                            "-fx-border-color:#E2E8F0; -fx-border-radius:10; -fx-border-width:1.5;");
            btnDetail.setOnAction(e -> afficherDetailMatching(dto));
            actionsRow.getChildren().add(btnDetail);
        }

        card.getChildren().addAll(topRow, sep, actionsRow);
        return card;
    }

    // ════════════════════════════════════════════════════════════
    //  MATCHING INDIVIDUEL (avec popup de résultat)
    // ════════════════════════════════════════════════════════════
    private void lancerMatchingIndividuel(CandidatureDTO dto, VBox card) {
        MatchingResult result = matchingService.calculerEtAppliquer(
                dto.getIdCandidature(), dto.getIdOffre(), dto.getIdCandidat());

        // Afficher une popup de résultat détaillé
        afficherPopupResultat(result, dto.getNomComplet());

        // Rafraîchir la liste
        chargerDonnees();
    }

    private void afficherPopupResultat(MatchingResult result, String nomCandidat) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/matching-result.fxml"));
            Parent root = loader.load();
            MatchingResultController ctrl = loader.getController();
            ctrl.afficherResultat(result, nomCandidat);
            Stage stage = new Stage();
            stage.setTitle("Résultat du Matching — " + nomCandidat);
            stage.setScene(new Scene(root));
            stage.setMinWidth(600);
            stage.setMinHeight(500);
            stage.showAndWait();
        } catch (IOException e) {
            // Fallback : alert simple si le FXML n'est pas encore créé
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Résultat du Matching");
            alert.setHeaderText(nomCandidat + " — " + result.decision);
            alert.setContentText(String.format(
                    "Score final        : %.1f%%\n" +
                            "Score compétences : %.1f%%\n" +
                            "Score expériences  : %.1f%%\n\n" +
                            "Décision : %s\nStatut mis à jour : %s",
                    result.scoreFinal, result.scoreCompetences, result.scoreExperiences,
                    result.decision, result.nouveauStatut));
            alert.showAndWait();
        }
    }

    private void afficherDetailMatching(CandidatureDTO dto) {
        // Recalcule le score sans modifier le statut (juste pour afficher)
        MatchingResult result = matchingService.calculerEtAppliquer(
                dto.getIdCandidature(), dto.getIdOffre(), dto.getIdCandidat());
        afficherPopupResultat(result, dto.getNomComplet());
    }

    // ════════════════════════════════════════════════════════════
    //  CHANGER STATUT MANUELLEMENT (cas EN_REVISION)
    // ════════════════════════════════════════════════════════════
    private void changerStatut(CandidatureDTO dto, String statut, VBox card) {
        String msg = "traité".equals(statut)
                ? "Accepter la candidature de " + dto.getNomComplet() + " ?"
                : "Rejeter la candidature de " + dto.getNomComplet() + " ?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                candidatureService.modifierStatut(dto.getIdCandidature(), statut);
                chargerDonnees();
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════════════════════
    private void mettreAJourStats() {
        if (toutesLesCandidatures == null) return;
        long total     = toutesLesCandidatures.size();
        long enAtt     = toutesLesCandidatures.stream().filter(c -> "en_attente".equalsIgnoreCase(c.getStatut())).count();
        long enRev     = toutesLesCandidatures.stream().filter(c -> "en_revision".equalsIgnoreCase(c.getStatut())).count();
        long accep     = toutesLesCandidatures.stream().filter(c -> "traité".equalsIgnoreCase(c.getStatut())).count();
        long ann       = toutesLesCandidatures.stream().filter(c -> "annulé".equalsIgnoreCase(c.getStatut())).count();

        if (totalLabel    != null) totalLabel.setText(String.valueOf(total));
        if (enAttenteLabel != null) enAttenteLabel.setText(String.valueOf(enAtt));
        if (revisionLabel != null) revisionLabel.setText(String.valueOf(enRev));
        if (acceptesLabel != null) acceptesLabel.setText(String.valueOf(accep));
        if (annulesLabel  != null) annulesLabel.setText(String.valueOf(ann));
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════════════════════
    private StackPane creerAvatar(String nomComplet, boolean bleu) {
        String[] parts = nomComplet.split(" ");
        String initials = parts.length >= 2
                ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                : nomComplet.substring(0, Math.min(2, nomComplet.length()));
        StackPane sp = new StackPane();
        Region bg = new Region();
        bg.setPrefSize(56, 56);
        bg.setStyle("-fx-background-color: " +
                (bleu ? "linear-gradient(to right,#2563EB,#1E40AF)"
                        : "linear-gradient(to right,#7C3AED,#5B21B6)") +
                "; -fx-background-radius:50;");
        Label lbl = new Label(initials.toUpperCase());
        lbl.setStyle("-fx-font-size:19px; -fx-font-weight:800; -fx-text-fill:white;");
        sp.getChildren().addAll(bg, lbl);
        sp.setPrefSize(56, 56); sp.setMaxSize(56, 56); sp.setMinSize(56, 56);
        return sp;
    }

    private Label creerBadgeStatut(String statut) {
        String txt, bg, fg;
        if (statut == null) { txt = "—"; bg = "#F3F4F6"; fg = "#6B7280"; }
        else switch (statut.toLowerCase()) {
            case "traité":      txt = "✅  Accepté";       bg = "#DCFCE7"; fg = "#15803D"; break;
            case "en_revision": txt = "🔍  En révision";   bg = "#FEF3C7"; fg = "#D97706"; break;
            case "annulé":      txt = "❌  Annulé";        bg = "#FEE2E2"; fg = "#DC2626"; break;
            case "en_attente":  txt = "⏳  En attente";    bg = "#EFF6FF"; fg = "#2563EB"; break;
            default:            txt = statut;              bg = "#F3F4F6"; fg = "#6B7280";
        }
        Label l = new Label(txt);
        l.setStyle(String.format(
                "-fx-background-color:%s; -fx-text-fill:%s; -fx-font-weight:800;" +
                        "-fx-font-size:12px; -fx-padding:7 16; -fx-background-radius:20;", bg, fg));
        return l;
    }

    private String bordureCouleur(String statut) {
        if (statut == null) return "";
        switch (statut.toLowerCase()) {
            case "traité":      return "-fx-border-color:#059669; -fx-border-width:0 0 0 5; -fx-border-radius:0 16 16 0;";
            case "en_revision": return "-fx-border-color:#D97706; -fx-border-width:0 0 0 5; -fx-border-radius:0 16 16 0;";
            case "annulé":      return "-fx-border-color:#DC2626; -fx-border-width:0 0 0 5; -fx-border-radius:0 16 16 0;";
            case "en_attente":  return "-fx-border-color:#2563EB; -fx-border-width:0 0 0 5; -fx-border-radius:0 16 16 0;";
            default:            return "";
        }
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION SIDEBAR
    // ════════════════════════════════════════════════════════════
    @FXML private void ouvrirOffresEmploi(ActionEvent e)  { naviguer(e, "/tn/jobnest/gentretien/offre-emploi_view.fxml",       "JobNest - Offres d'Emploi"); }
    @FXML private void ouvrirCandidature(ActionEvent e)   { naviguer(e, "/tn/jobnest/gentretien/GestionCandidatures.fxml",     "JobNest - Candidatures"); }
    @FXML private void ouvrirEntretiens(ActionEvent e)    { naviguer(e, "/tn/jobnest/gentretien/entretien-view.fxml",          "JobNest - Entretiens"); }
    @FXML private void ouvrirFeedbacks(ActionEvent e)     { naviguer(e, "/tn/jobnest/gentretien/feedback-interface.fxml",      "JobNest - Feedbacks"); }
    @FXML private void ouvrirHistorique(ActionEvent e)    { naviguer(e, "/tn/jobnest/gentretien/historique-entretien.fxml",    "JobNest - Historique"); }
    @FXML private void ouvrirProfil(ActionEvent e)        { naviguer(e, "/tn/jobnest/gentretien/profil-recruteur.fxml",        "JobNest - Profil"); }

    private void naviguer(ActionEvent event, String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            var css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
            stage.show();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur navigation : " + ex.getMessage()).showAndWait();
        }
    }
}