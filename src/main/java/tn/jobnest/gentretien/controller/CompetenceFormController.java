package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.service.CompetenceService;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CompetenceFormController {

    @FXML private TextField        nomField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private TextArea         descriptionField;
    @FXML private Button           btnSave;

    private final CompetenceService service = new CompetenceService();
    private Competence competence;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        categorieCombo.getItems().addAll("technique", "soft_skill", "langue", "autre");
        niveauCombo.getItems().addAll("debutant", "intermediaire", "avance", "expert");

        nomField.textProperty().addListener((o, a, b) -> validate());
        descriptionField.textProperty().addListener((o, a, b) -> validate());
        categorieCombo.valueProperty().addListener((o, a, b) -> validate());
        niveauCombo.valueProperty().addListener((o, a, b) -> validate());

        validate();
    }

    // ================= GETTER =================
    public Competence getCompetence() {
        return competence;
    }

    // ================= SETTER (EDIT MODE) =================
    public void setCompetence(Competence c) {
        this.competence = c;

        nomField.setText(c.getNom());
        categorieCombo.setValue(c.getCategorie());
        niveauCombo.setValue(c.getNiveauRequis());
        descriptionField.setText(c.getDescription());
        btnSave.setText("Modifier");

        validate();
    }

    // ================= CLOSE POPUP ================= ← MÉTHODE MANQUANTE AJOUTÉE
    @FXML
    private void closePopup() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }

    // ================= SAVE =================
    @FXML
    private void save() {
        try {
            if (competence == null) {
                // AJOUT
                competence = new Competence(
                        nomField.getText().trim(),
                        categorieCombo.getValue(),
                        descriptionField.getText().trim(),
                        niveauCombo.getValue()
                );
                service.ajouter(competence);
                showSuccess("Compétence ajoutée ✅");
            } else {
                // MODIFICATION
                competence.setNom(nomField.getText().trim());
                competence.setCategorie(categorieCombo.getValue());
                competence.setNiveauRequis(niveauCombo.getValue());
                competence.setDescription(descriptionField.getText().trim());
                service.modifier(competence);
                showSuccess("Compétence modifiée ✅");
            }

            ((Stage) btnSave.getScene().getWindow()).close();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= VALIDATION =================
    private void validate() {
        boolean nomOk  = nomField.getText() != null && nomField.getText().trim().length() >= 3;
        boolean descOk = descriptionField.getText() != null && descriptionField.getText().trim().length() >= 5;
        boolean catOk  = categorieCombo.getValue() != null;
        boolean nivOk  = niveauCombo.getValue() != null;

        style(nomField,         nomOk);
        style(descriptionField, descOk);
        style(categorieCombo,   catOk);
        style(niveauCombo,      nivOk);

        if (btnSave != null)
            btnSave.setDisable(!(nomOk && descOk && catOk && nivOk));
    }

    private void style(Control c, boolean ok) {
        c.setStyle(ok
                ? "-fx-border-color:#22c55e; -fx-border-width:1.5;"
                : "-fx-border-color:#ef4444; -fx-border-width:1.5;");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}