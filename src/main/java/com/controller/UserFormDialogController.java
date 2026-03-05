package com.controller;

import com.controller.AdminDashboardController.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class UserFormDialogController {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private TextArea adresseField;
    @FXML private ComboBox<String> roleCombo;

    private Stage dialogStage;
    private User user;
    private boolean isValid = false;

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("Candidat", "Recruteur", "Formateur", "Admin");
        roleCombo.setValue("Candidat");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setUser(User user) {
        this.user = user;

        // Remplir les champs avec les données existantes
        prenomField.setText(user.getPrenom());
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone());
        adresseField.setText(user.getAdresse());
        roleCombo.setValue(user.getRole());

        // Masquer le champ mot de passe en mode édition
        passwordField.setVisible(false);
        passwordField.setManaged(false);
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            if (user == null) {
                // Nouveau user
                user = new User(0,
                        prenomField.getText().trim(),
                        nomField.getText().trim(),
                        emailField.getText().trim(),
                        roleCombo.getValue(),
                        telephoneField.getText().trim(),
                        null
                );
                user.setMotDePasse(passwordField.getText().trim());
                user.setAdresse(adresseField.getText().trim());
            } else {
                // Modification
                user.setPrenom(prenomField.getText().trim());
                user.setNom(nomField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setRole(roleCombo.getValue());
                user.setTelephone(telephoneField.getText().trim());
                user.setAdresse(adresseField.getText().trim());
            }

            isValid = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        isValid = false;
        dialogStage.close();
    }

    private boolean validateInput() {
        String errorMessage = "";

        if (prenomField.getText() == null || prenomField.getText().trim().isEmpty()) {
            errorMessage += "Prénom invalide!\n";
        }
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errorMessage += "Nom invalide!\n";
        }
        if (emailField.getText() == null || !emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorMessage += "Email invalide!\n";
        }
        if (user == null && (passwordField.getText() == null || passwordField.getText().length() < 6)) {
            errorMessage += "Mot de passe invalide (min 6 caractères)!\n";
        }
        if (telephoneField.getText() == null || !telephoneField.getText().matches("\\d{8,15}")) {
            errorMessage += "Téléphone invalide (8-15 chiffres)!\n";
        }
        if (roleCombo.getValue() == null) {
            errorMessage += "Rôle non sélectionné!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Champs invalides");
            alert.setHeaderText("Veuillez corriger les erreurs");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public User getUser() {
        return user;
    }
}