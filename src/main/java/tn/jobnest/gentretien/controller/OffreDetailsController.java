package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.service.OffreEmploiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class OffreDetailsController {

    // ── Labels principaux ──
    @FXML private Label    titreLabel;
    @FXML private Label    statutBadgeLabel;

    // ── Informations générales ──
    @FXML private Label    entrepriseLabel;
    @FXML private Label    contratLabel;
    @FXML private Label    nbPostesLabel;

    // ── Rémunération ──
    @FXML private Label    salaireLabel;

    // ── Dates ──
    @FXML private Label    publicationLabel;
    @FXML private Label    expirationLabel;

    // ── Description ──
    @FXML private Label    descriptionLabel;

    // ── Tags ──
    @FXML private FlowPane competencesPane;
    @FXML private FlowPane experiencesPane;

    // ── Statistiques ──
    @FXML private Label    nbVuesLabel;
    @FXML private Label    nbCandidaturesLabel;

    // ── Boutons d'action ──
    @FXML private Button   fermerBtn;
    @FXML private Button   republierBtn;

    // ── Services & données ──
    private OffreEmploi             offre;
    private final OffreEmploiService service = new OffreEmploiService();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ============================================================
    //  SETTER PRINCIPAL — appelé depuis OffreEmploiController
    // ============================================================
    public void setOffre(OffreEmploi o) {
        this.offre = o;
        if (o == null) return;

        titreLabel.setText(val(o.getTitre()));

        boolean ouvert = isOffreOuverte(o);
        rafraichirStatutUI(ouvert);

        entrepriseLabel.setText(val(o.getEntreprise()));
        contratLabel.setText(val(o.getTypeContrat()));
        nbPostesLabel.setText(o.getNbPostes() > 0
                ? o.getNbPostes() + " poste(s)" : "Non précisé");

        // Salaire
        double sMin = o.getSalaireMin(), sMax = o.getSalaireMax();
        if (sMin > 0 && sMax > 0 && sMin != sMax)
            salaireLabel.setText((int) sMin + " – " + (int) sMax + " DT / mois");
        else if (sMin > 0)
            salaireLabel.setText((int) sMin + " DT / mois");
        else
            salaireLabel.setText("Non précisé");

        publicationLabel.setText(formatDate(o.getDatePublication()));
        expirationLabel.setText(formatDate(o.getDateExpiration()));
        descriptionLabel.setText(val(o.getDescription()));

        nbVuesLabel.setText(String.valueOf(o.getNbVues()));
        nbCandidaturesLabel.setText(String.valueOf(o.getNbCandidatures()));

        // Tags compétences
        if (competencesPane != null) {
            competencesPane.getChildren().clear();
            if (o.getCompetences() != null && !o.getCompetences().isEmpty()) {
                for (Competence c : o.getCompetences()) {
                    if (c == null || c.getNom() == null) continue;
                    Label tag = new Label(c.getNom());
                    tag.setStyle(
                            "-fx-background-color:#DBEAFE; -fx-text-fill:#1E40AF;" +
                                    "-fx-font-size:12px; -fx-font-weight:bold;" +
                                    "-fx-background-radius:20; -fx-padding:4 12 4 12;");
                    competencesPane.getChildren().add(tag);
                }
            } else {
                Label none = new Label("Aucune compétence associée");
                none.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;" +
                        "-fx-font-style:italic;");
                competencesPane.getChildren().add(none);
            }
        }

        // Tags expériences
        if (experiencesPane != null) {
            experiencesPane.getChildren().clear();
            if (o.getExperiences() != null && !o.getExperiences().isEmpty()) {
                for (Experience exp : o.getExperiences()) {
                    if (exp == null || exp.getNom() == null) continue;
                    Label tag = new Label(exp.getNom());
                    tag.setStyle(
                            "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46;" +
                                    "-fx-font-size:12px; -fx-font-weight:bold;" +
                                    "-fx-background-radius:20; -fx-padding:4 12 4 12;");
                    experiencesPane.getChildren().add(tag);
                }
            } else {
                Label none = new Label("Aucune expérience associée");
                none.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;" +
                        "-fx-font-style:italic;");
                experiencesPane.getChildren().add(none);
            }
        }
    }

    // ============================================================
    //  ACTION : FERMER L'OFFRE
    // ============================================================
    @FXML
    private void handleFermerOffre() {
        if (offre == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Fermer l'offre");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(
                "Fermer « " + offre.getTitre() + " » ?\n\n" +
                        "Les candidats ne pourront plus postuler à cette offre.");

        ButtonType btnFermer  = new ButtonType(
                "🔒  Fermer l'offre", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType(
                "Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnFermer, btnAnnuler);
        alert.getDialogPane().lookupButton(btnFermer).setStyle(
                "-fx-background-color:#EA580C; -fx-text-fill:white;" +
                        "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnFermer) {
            try {
                service.fermerOffre(offre.getIdOffre());
                offre.setStatut("fermee");
                rafraichirStatutUI(false);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Succès");
                ok.setHeaderText(null);
                ok.setContentText("✅  L'offre a été fermée avec succès.");
                ok.showAndWait();

            } catch (SQLException e) {
                showErreur("Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }

    // ============================================================
    //  ACTION : REPUBLIER L'OFFRE
    // ============================================================
    @FXML
    private void handleRepublierOffre() {
        if (offre == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/tn/jobnest/gentretien/offre_form.fxml"));
            Parent root = loader.load();
            OffreFormController ctrl = loader.getController();

            // Copie avec date d'expiration nulle pour forcer une nouvelle saisie
            OffreEmploi copie = clonerPourRepublication(offre);
            ctrl.setOffre(copie);
            ctrl.setModeRepublication(true);

            Stage stage = new Stage();
            stage.setTitle("🔄 Republier — " + offre.getTitre());
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

                // Mettre à jour l'UI sans fermer la fenêtre
                offre.setStatut("publiee");
                offre.setDateExpiration(offreModifiee.getDateExpiration());
                expirationLabel.setText(formatDate(offre.getDateExpiration()));
                rafraichirStatutUI(true);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Republié !");
                ok.setHeaderText(null);
                ok.setGraphic(null);
                ok.setContentText(
                        "✅  L'offre « " + offre.getTitre() +
                                " » a été republiée avec succès !\n" +
                                "Elle est à nouveau ouverte aux candidatures.");
                ok.showAndWait();
            }

        } catch (IOException | SQLException e) {
            showErreur("Erreur lors de la republication : " + e.getMessage());
        }
    }

    // ============================================================
    //  MISE À JOUR UI SELON STATUT
    // ============================================================
    private void rafraichirStatutUI(boolean ouvert) {
        // Badge statut
        if (statutBadgeLabel != null) {
            statutBadgeLabel.setText(ouvert ? "● Ouvert" : "● Fermé");
            statutBadgeLabel.setStyle(ouvert
                    ? "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;" +
                    "-fx-font-size:12px; -fx-font-weight:bold;" +
                    "-fx-background-radius:20; -fx-padding:4 12 4 12;"
                    : "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;" +
                    "-fx-font-size:12px; -fx-font-weight:bold;" +
                    "-fx-background-radius:20; -fx-padding:4 12 4 12;");
        }
        // Bouton Fermer : visible seulement si ouvert
        if (fermerBtn != null) {
            fermerBtn.setVisible(ouvert);
            fermerBtn.setManaged(ouvert);
        }
        // Bouton Republier : visible seulement si fermé
        if (republierBtn != null) {
            republierBtn.setVisible(!ouvert);
            republierBtn.setManaged(!ouvert);
        }
    }

    // ============================================================
    //  HELPERS
    // ============================================================

    /** Clone l'offre avec date_expiration = null pour la republication */
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
        c.setDateExpiration(null); // Force une nouvelle date d'expiration
        c.setStatut("publiee");
        return c;
    }

    private boolean isOffreOuverte(OffreEmploi o) {
        if ("fermee".equals(o.getStatut())) return false;
        if (o.getDateExpiration() == null)   return true;
        return o.getDateExpiration().toLocalDate().isAfter(LocalDate.now());
    }

    private String val(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    private String formatDate(java.sql.Date d) {
        if (d == null) return "—";
        try   { return d.toLocalDate().format(DATE_FMT); }
        catch (Exception e) { return d.toString(); }
    }

    private void showErreur(String msg) {
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle("Erreur");
        err.setHeaderText(null);
        err.setContentText(msg);
        err.showAndWait();
    }

    @FXML
    private void closePopup() {
        ((Stage) titreLabel.getScene().getWindow()).close();
    }
}