package tn.jobnest.gentretien.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.model.Feedback;
import tn.jobnest.gentretien.service.FeedbackService;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

public class FeedbackFormController {

    @FXML private Slider sliderCompetenceTechniques;
    @FXML private Slider sliderCompetenceCommunication;
    @FXML private Slider sliderMotivation;
    @FXML private Slider sliderAdequationPoste;
    @FXML private Label labelCompetenceTechniques;
    @FXML private Label labelCompetenceCommunication;
    @FXML private Label labelMotivation;
    @FXML private Label labelAdequationPoste;
    @FXML private TextArea commentaire;
    @FXML private TextField competenceManquantes;
    @FXML private CheckBox suggestionFormation;
    @FXML private Label labelEntretienInfo;

    private Entretien entretien;
    private final FeedbackService feedbackService = new FeedbackService();
    private Feedback feedbackExistant;

    @FXML
    private void initialize() {
        // Lier les sliders aux labels pour afficher les valeurs
        sliderCompetenceTechniques.valueProperty().addListener((obs, oldVal, newVal) ->
                labelCompetenceTechniques.setText(String.valueOf(newVal.intValue()) + "/10"));

        sliderCompetenceCommunication.valueProperty().addListener((obs, oldVal, newVal) ->
                labelCompetenceCommunication.setText(String.valueOf(newVal.intValue()) + "/10"));

        sliderMotivation.valueProperty().addListener((obs, oldVal, newVal) ->
                labelMotivation.setText(String.valueOf(newVal.intValue()) + "/10"));

        sliderAdequationPoste.valueProperty().addListener((obs, oldVal, newVal) ->
                labelAdequationPoste.setText(String.valueOf(newVal.intValue()) + "/10"));

        // Initialiser les valeurs par défaut
        sliderCompetenceTechniques.setValue(5);
        sliderCompetenceCommunication.setValue(5);
        sliderMotivation.setValue(5);
        sliderAdequationPoste.setValue(5);
    }

    public void setEntretien(Entretien entretien) {
        this.entretien = entretien;

        // Afficher les informations de l'entretien
        if (entretien != null) {
            try {
                String info = "Entretien #" + entretien.getIdEntretien() +
                        " - " + entretien.getDateEntretien();
                labelEntretienInfo.setText(info);

                // Vérifier si un feedback existe déjà
                feedbackExistant = feedbackService.getFeedbackByEntretien(entretien.getIdEntretien());

                if (feedbackExistant != null) {
                    // Pré-remplir le formulaire avec les données existantes
                    chargerFeedback(feedbackExistant);
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de charger le feedback : " + e.getMessage());
            }
        }
    }

    /**
     * Méthode pour forcer le chargement d'un feedback existant (pour modification)
     */
    public void setFeedbackExistant(Feedback feedback) {
        this.feedbackExistant = feedback;
        if (feedback != null) {
            chargerFeedback(feedback);
        }
    }

    private void chargerFeedback(Feedback feedback) {
        sliderCompetenceTechniques.setValue(feedback.getCompetenceTechniques());
        sliderCompetenceCommunication.setValue(feedback.getCompetenceCommunication());
        sliderMotivation.setValue(feedback.getMotivation());
        sliderAdequationPoste.setValue(feedback.getAdequationAuPoste());
        commentaire.setText(feedback.getCommentaire());
        competenceManquantes.setText(feedback.getCompetenceManquantes());
        suggestionFormation.setSelected(feedback.isSuggestionFormation());
    }

    @FXML
    private void save() {
        // ===== VALIDATION =====
        if (entretien == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Aucun entretien n'est associé à ce feedback.");
            return;
        }

        if (commentaire.getText() == null || commentaire.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez saisir un commentaire.");
            commentaire.requestFocus();
            return;
        }

        if (competenceManquantes.getText() == null || competenceManquantes.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez indiquer les compétences manquantes (ou indiquer 'Aucune').");
            competenceManquantes.requestFocus();
            return;
        }

        // ===== ENREGISTREMENT =====
        try {
            Feedback feedback;

            if (feedbackExistant != null) {
                // Mise à jour d'un feedback existant
                feedback = feedbackExistant;
            } else {
                // Création d'un nouveau feedback
                feedback = new Feedback();
                feedback.setIdEntretien(entretien.getIdEntretien());
            }

            feedback.setCompetenceTechniques((int) sliderCompetenceTechniques.getValue());
            feedback.setCompetenceCommunication((int) sliderCompetenceCommunication.getValue());
            feedback.setMotivation((int) sliderMotivation.getValue());
            feedback.setAdequationAuPoste((int) sliderAdequationPoste.getValue());
            feedback.setCommentaire(commentaire.getText().trim());
            feedback.setCompetenceManquantes(competenceManquantes.getText().trim());
            feedback.setSuggestionFormation(suggestionFormation.isSelected());
            feedback.setDateFeedback(new Timestamp(System.currentTimeMillis()));

            if (feedbackExistant != null) {
                feedbackService.update(feedback);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Feedback modifié avec succès !");
            } else {
                feedbackService.ajouter(feedback);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Feedback enregistré avec succès !");
            }

            // Fermer la fenêtre
            Stage stage = (Stage) commentaire.getScene().getWindow();
            stage.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible d'enregistrer le feedback : " + ex.getMessage());
        }
    }

    @FXML
    private void annuler() {
        Stage stage = (Stage) commentaire.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        try {
            // Chargez le fichier FXML de l'interface des candidatures
            // Assurez-vous que le chemin vers le FXML est correct
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/GestionCandidatures.fxml"));
            Parent root = loader.load();

            // Récupérer le Stage (la fenêtre) actuel
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);

            // Optionnel : Ajouter votre fichier CSS
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Candidatures");
            stage.show();

        } catch (IOException ex) {
            // Utilisation de votre méthode showAlert existante
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'interface des candidatures : " + ex.getMessage());
        }
    }
}