package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.voice.VoiceBridge;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

public class ExperienceFormController {

    @FXML private TextField        nomField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private TextArea         descriptionField;
    @FXML private ComboBox<String> niveauBox;

    private Experience experience;

    // ============================
    //  INITIALIZE
    // ============================
    @FXML
    public void initialize() {
        categorieBox.setItems(FXCollections.observableArrayList(
                "technique", "management", "stage", "projet", "autre"));
        niveauBox.setItems(FXCollections.observableArrayList(
                "debutant", "intermediaire", "avance", "expert"));

        // Voice bridge
        VoiceBridge.register(text -> Platform.runLater(() -> {
            if (text == null || text.isBlank()) return;
            String t = text.toLowerCase();
            descriptionField.setText(text);
            if (t.contains("java"))   nomField.setText("Développeur Java");
            if (t.contains("web"))    nomField.setText("Développeur Web");
            if (t.contains("stage"))  nomField.setText("Stage Développeur");
            if (t.contains("projet")) nomField.setText("Projet Informatique");

            if      (t.contains("java") || t.contains("technique")) categorieBox.setValue("technique");
            else if (t.contains("management"))                       categorieBox.setValue("management");
            else if (t.contains("stage"))                            categorieBox.setValue("stage");
            else if (t.contains("projet"))                           categorieBox.setValue("projet");

            if      (t.contains("expert"))         niveauBox.setValue("expert");
            else if (t.contains("avance"))         niveauBox.setValue("avance");
            else if (t.contains("intermediaire"))  niveauBox.setValue("intermediaire");
            else                                   niveauBox.setValue("debutant");
        }));
    }

    // ============================
    //  SAVE
    // ============================
    @FXML
    private void handleSave() {
        if (!validerFormulaire()) return;
        if (!openCaptcha()) return;

        if (experience == null) experience = new Experience();
        experience.setNom(nomField.getText().trim());
        experience.setCategorie(categorieBox.getValue());
        experience.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : "");
        experience.setNiveauRequis(niveauBox.getValue());

        closePopup();
    }

    // ============================
    //  CAPTCHA
    //  ✅ FIX : URL résolue via getClass().getResource() — même approche que handleAddExperience
    // ============================
    private boolean openCaptcha() {
        try {
            URL fxmlUrl = getClass().getResource("/tn/jobnest/gentretien/captcha_slider.fxml");

            if (fxmlUrl == null) {
                // captcha_slider.fxml absent → on laisse passer sans bloquer
                System.err.println("[Captcha] captcha_slider.fxml introuvable, validation ignorée.");
                return true;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Vérification sécurité");
            stage.showAndWait();

            SliderCaptchaController ctrl = loader.getController();
            return ctrl != null && ctrl.isValidated();

        } catch (Exception e) {
            System.err.println("[Captcha] Erreur chargement : " + e.getMessage());
            return true; // Ne pas bloquer si erreur
        }
    }

    // ============================
    //  GETTERS / SETTERS
    // ============================
    public Experience getExperience() { return experience; }

    public void setExperience(Experience e) {
        this.experience = e;
        if (e == null) return;
        nomField.setText(e.getNom());
        categorieBox.setValue(e.getCategorie());
        if (descriptionField != null) descriptionField.setText(e.getDescription());
        niveauBox.setValue(e.getNiveauRequis());
    }

    // ============================
    //  VALIDATION
    // ============================
    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();
        if (nomField.getText() == null || nomField.getText().isBlank())
            errors.append("• Nom obligatoire\n");
        if (categorieBox.getValue() == null)
            errors.append("• Catégorie obligatoire\n");
        if (niveauBox.getValue() == null)
            errors.append("• Niveau obligatoire\n");

        if (errors.length() > 0) {
            new Alert(Alert.AlertType.ERROR, errors.toString()).showAndWait();
            return false;
        }
        return true;
    }

    // ============================
    //  VOICE
    // ============================
    @FXML
    private void startVoice() {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:5555/voice.html"));
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING,
                    "Impossible d'ouvrir le navigateur : " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void closePopup() {
        if (nomField != null && nomField.getScene() != null) {
            ((Stage) nomField.getScene().getWindow()).close();
        }
    }
}