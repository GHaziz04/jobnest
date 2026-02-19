package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.model.Feedback;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FeedbackDetailController {

    @FXML private Label headerTitreOffre;
    @FXML private Label headerEntretienId;
    @FXML private Label headerCandidat;
    @FXML private Label headerDate;

    @FXML private Label moyenneGlobaleLabel;
    @FXML private Label moyenneMentionLabel;

    @FXML private Label noteTechLabel;
    @FXML private Label noteCommLabel;
    @FXML private Label noteMotivLabel;
    @FXML private Label noteAdequLabel;

    @FXML private HBox barTech;
    @FXML private HBox barComm;
    @FXML private HBox barMotiv;
    @FXML private HBox barAdequ;

    @FXML private Label commentaireLabel;
    @FXML private Label competencesLabel;
    @FXML private Label formationLabel;
    @FXML private Label dateFeedbackLabel;

    @FXML private VBox formationBox;

    public void setData(Feedback f, Entretien entretien, List<String> participants, String titreOffre) {
        // ---- Header ----
        headerTitreOffre.setText(titreOffre != null ? titreOffre.toUpperCase() : "Offre inconnue");
        headerEntretienId.setText("Entretien #" + entretien.getIdEntretien());

        String candidatNom = participants.isEmpty() ? "Candidat inconnu" : participants.get(0);
        headerCandidat.setText("👤  " + candidatNom);

        String dateEntretien = (entretien.getDateEntretien() != null)
                ? entretien.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Date inconnue";
        headerDate.setText("📅  " + dateEntretien);

        // ---- Moyenne globale ----
        double moyenne = (f.getCompetenceTechniques() + f.getCompetenceCommunication()
                + f.getMotivation() + f.getAdequationAuPoste()) / 4.0;

        moyenneGlobaleLabel.setText(String.format("%.1f", moyenne));
        moyenneGlobaleLabel.setStyle(moyenneGlobaleLabel.getStyle() + "-fx-text-fill: " + getCouleurNote(moyenne) + ";");

        String mention;
        if (moyenne >= 8.0)      mention = "Excellent";
        else if (moyenne >= 6.0) mention = "Bien";
        else if (moyenne >= 4.0) mention = "Moyen";
        else                     mention = "Faible";
        moyenneMentionLabel.setText(mention);
        moyenneMentionLabel.setStyle(
                "-fx-text-fill: " + getCouleurNote(moyenne) + ";" +
                        "-fx-font-size: 13px; -fx-font-weight: 700;" +
                        "-fx-background-color: " + getCouleurNoteLight(moyenne) + ";" +
                        "-fx-background-radius: 20px; -fx-padding: 3 12 3 12;"
        );

        // ---- Notes individuelles + barres ----
        setNoteBar(noteTechLabel,  barTech,  f.getCompetenceTechniques());
        setNoteBar(noteCommLabel,  barComm,  f.getCompetenceCommunication());
        setNoteBar(noteMotivLabel, barMotiv, f.getMotivation());
        setNoteBar(noteAdequLabel, barAdequ, f.getAdequationAuPoste());

        // ---- Commentaire ----
        commentaireLabel.setText(f.getCommentaire() != null && !f.getCommentaire().isEmpty()
                ? f.getCommentaire() : "Aucun commentaire.");

        // ---- Compétences manquantes ----
        competencesLabel.setText(f.getCompetenceManquantes() != null && !f.getCompetenceManquantes().isEmpty()
                ? f.getCompetenceManquantes() : "Aucune compétence manquante identifiée.");

        // ---- Formation ----
        if (f.isSuggestionFormation()) {
            formationBox.setVisible(true);
            formationBox.setManaged(true);
            formationLabel.setText("✅ Une formation est recommandée pour ce candidat.");
        } else {
            formationBox.setVisible(false);
            formationBox.setManaged(false);
        }

        // ---- Date feedback ----
        String dateFb = (f.getDateFeedback() != null)
                ? f.getDateFeedback().toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm"))
                : "Date inconnue";
        dateFeedbackLabel.setText("Feedback enregistré le " + dateFb);
    }

    private void setNoteBar(Label noteLabel, HBox barContainer, int note) {
        noteLabel.setText(note + "/10");
        noteLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + getCouleurNote(note) + ";");

        barContainer.getChildren().clear();
        barContainer.setSpacing(3);

        for (int i = 1; i <= 10; i++) {
            javafx.scene.layout.Region segment = new javafx.scene.layout.Region();
            segment.setPrefWidth(22);
            segment.setPrefHeight(10);
            String radius = (i == 1) ? "5 0 0 5" : (i == 10) ? "0 5 5 0" : "0";
            if (i <= note) {
                segment.setStyle(
                        "-fx-background-color: " + getCouleurNote(note) + ";" +
                                "-fx-background-radius: " + radius + ";"
                );
            } else {
                segment.setStyle(
                        "-fx-background-color: #E2E8F0;" +
                                "-fx-background-radius: " + radius + ";"
                );
            }
            barContainer.getChildren().add(segment);
        }
    }

    private String getCouleurNote(double note) {
        if (note >= 8.0) return "#059669";
        if (note >= 6.0) return "#2563EB";
        if (note >= 4.0) return "#D97706";
        return "#DC2626";
    }

    private String getCouleurNoteLight(double note) {
        if (note >= 8.0) return "#D1FAE5";
        if (note >= 6.0) return "#DBEAFE";
        if (note >= 4.0) return "#FEF3C7";
        return "#FEE2E2";
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) headerTitreOffre.getScene().getWindow();
        stage.close();
    }
}