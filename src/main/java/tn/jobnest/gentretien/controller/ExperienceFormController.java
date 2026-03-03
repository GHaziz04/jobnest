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

public class ExperienceFormController {

    @FXML private TextField        nomField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private TextArea         descriptionField;
    @FXML private ComboBox<String> niveauBox;

    private Experience experience;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        categorieBox.setItems(FXCollections.observableArrayList(
                "technique", "management", "stage", "projet", "autre"
        ));
        niveauBox.setItems(FXCollections.observableArrayList(
                "debutant", "intermediaire", "avance", "expert"
        ));

        // Écoute la reconnaissance vocale
        VoiceBridge.register(text -> Platform.runLater(() -> {

            if (text == null || text.isBlank()) return;

            String t = text.toLowerCase();
            descriptionField.setText(text);

            if (t.contains("stage"))       nomField.setText("Stage Développeur");
            if (t.contains("projet"))      nomField.setText("Projet Informatique");
            if (t.contains("java"))        nomField.setText("Développeur Java");
            if (t.contains("web"))         nomField.setText("Développeur Web");

            if (t.contains("java") || t.contains("technique"))  categorieBox.setValue("technique");
            if (t.contains("management"))                        categorieBox.setValue("management");
            if (t.contains("stage"))                             categorieBox.setValue("stage");
            if (t.contains("projet"))                            categorieBox.setValue("projet");

            if (t.contains("debutant"))      niveauBox.setValue("debutant");
            if (t.contains("intermediaire")) niveauBox.setValue("intermediaire");
            if (t.contains("avance"))        niveauBox.setValue("avance");
            if (t.contains("expert"))        niveauBox.setValue("expert");
        }));
    }

    // ================= SAVE =================
    @FXML
    private void handleSave() {

        if (!validerFormulaire()) return;
        if (!openCaptcha()) return;

        if (experience == null) experience = new Experience();

        experience.setNom(nomField.getText().trim());
        experience.setCategorie(categorieBox.getValue());
        experience.setDescription(descriptionField.getText().trim());
        experience.setNiveauRequis(niveauBox.getValue());

        closePopup();
    }

    // ================= CAPTCHA =================
    private boolean openCaptcha() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/captcha_slider.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Vérification sécurité");
            stage.showAndWait();
            SliderCaptchaController ctrl = loader.getController();
            return ctrl.isValidated();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= GETTERS / SETTERS =================
    public Experience getExperience() { return experience; }

    public void setExperience(Experience e) {
        this.experience = e;
        if (e != null) {
            nomField.setText(e.getNom());
            categorieBox.setValue(e.getCategorie());
            descriptionField.setText(e.getDescription());
            niveauBox.setValue(e.getNiveauRequis());
        }
    }

    // ================= VALIDATION =================
    private boolean validerFormulaire() {

        StringBuilder errors = new StringBuilder();
        if (nomField.getText().isBlank())          errors.append("• Nom obligatoire\n");
        if (categorieBox.getValue() == null)       errors.append("• Catégorie obligatoire\n");
        if (descriptionField.getText().isBlank())  errors.append("• Description obligatoire\n");
        if (niveauBox.getValue() == null)          errors.append("• Niveau obligatoire\n");

        if (errors.length() > 0) {
            new Alert(Alert.AlertType.ERROR, errors.toString()).showAndWait();
            return false;
        }
        return true;
    }

    // ================= VOICE =================
    @FXML
    private void startVoice() {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:5555/voice.html"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closePopup() {
        ((Stage) nomField.getScene().getWindow()).close();
    }
}