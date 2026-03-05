package com.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ChangePasswordDialogController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    @FXML private TextField currentPasswordVisible;
    @FXML private TextField newPasswordVisible;
    @FXML private TextField confirmPasswordVisible;
    
    @FXML private Button toggleCurrentBtn;
    @FXML private Button toggleNewBtn;
    @FXML private Button toggleConfirmBtn;
    
    @FXML private Label currentPasswordError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;
    
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label strengthLabel;

    private boolean currentVisible = false;
    private boolean newVisible = false;
    private boolean confirmVisible = false;
    
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
    private boolean isValid = false;

    @FXML
    public void initialize() {
        // Validation en temps réel du nouveau mot de passe
        newPasswordField.textProperty().addListener((obs, old, newVal) -> {
            updatePasswordStrength(newVal);
            validateNewPassword();
            if (!confirmPasswordField.getText().isEmpty()) {
                validateConfirmPassword();
            }
        });

        // Validation de la confirmation
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            validateConfirmPassword();
        });

        // Validation du mot de passe actuel
        currentPasswordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) {
                currentPasswordError.setVisible(false);
                currentPasswordError.setManaged(false);
            }
        });
    }

    // ===== TOGGLE PASSWORD VISIBILITY =====
    @FXML
    private void toggleCurrentPassword() {
        currentVisible = !currentVisible;
        togglePasswordField(currentPasswordField, currentVisible, toggleCurrentBtn);
    }

    @FXML
    private void toggleNewPassword() {
        newVisible = !newVisible;
        togglePasswordField(newPasswordField, newVisible, toggleNewBtn);
    }

    @FXML
    private void toggleConfirmPassword() {
        confirmVisible = !confirmVisible;
        togglePasswordField(confirmPasswordField, confirmVisible, toggleConfirmBtn);
    }

    private void togglePasswordField(PasswordField field, boolean visible, Button btn) {
        if (visible) {
            field.setPromptText(field.getText());
            field.setText(""); // Vider temporairement
            field.setPromptText(field.getPromptText());
            btn.setText("🙈");
        } else {
            btn.setText("👁");
        }
    }

    // ===== PASSWORD STRENGTH =====
    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);
        double progress = strength / 100.0;
        passwordStrengthBar.setProgress(progress);

        String style = "";
        String label = "";

        if (strength < 25) {
            style = "-fx-accent: #E74C3C;"; // Rouge
            label = "Faible";
            strengthLabel.setStyle("-fx-text-fill: #E74C3C;");
        } else if (strength < 50) {
            style = "-fx-accent: #E67E22;"; // Orange
            label = "Moyen";
            strengthLabel.setStyle("-fx-text-fill: #E67E22;");
        } else if (strength < 75) {
            style = "-fx-accent: #F39C12;"; // Jaune
            label = "Bon";
            strengthLabel.setStyle("-fx-text-fill: #F39C12;");
        } else {
            style = "-fx-accent: #27AE60;"; // Vert
            label = "Fort";
            strengthLabel.setStyle("-fx-text-fill: #27AE60;");
        }

        passwordStrengthBar.setStyle(style);
        strengthLabel.setText(label);
    }

    private int calculatePasswordStrength(String password) {
        if (password.isEmpty()) return 0;

        int strength = 0;

        // Longueur
        if (password.length() >= 6) strength += 20;
        if (password.length() >= 8) strength += 15;
        if (password.length() >= 12) strength += 15;

        // Contient des minuscules
        if (password.matches(".*[a-z].*")) strength += 10;

        // Contient des majuscules
        if (password.matches(".*[A-Z].*")) strength += 15;

        // Contient des chiffres
        if (password.matches(".*\\d.*")) strength += 15;

        // Contient des caractères spéciaux
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength += 10;

        return Math.min(strength, 100);
    }

    // ===== VALIDATION =====
    private boolean validateNewPassword() {
        String password = newPasswordField.getText();

        if (password.isEmpty()) {
            showError(newPasswordError, "Le nouveau mot de passe est requis");
            return false;
        }

        if (password.length() < 6) {
            showError(newPasswordError, "Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }

        hideError(newPasswordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        String confirm = confirmPasswordField.getText();
        String newPass = newPasswordField.getText();

        if (confirm.isEmpty()) {
            showError(confirmPasswordError, "Veuillez confirmer le mot de passe");
            return false;
        }

        if (!confirm.equals(newPass)) {
            showError(confirmPasswordError, "Les mots de passe ne correspondent pas");
            return false;
        }

        hideError(confirmPasswordError);
        return true;
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    // ===== ACTIONS =====
    @FXML
    private void handleSave() {
        // Valider tous les champs
        boolean currentValid = !currentPasswordField.getText().isEmpty();
        boolean newValid = validateNewPassword();
        boolean confirmValid = validateConfirmPassword();

        if (!currentValid) {
            showError(currentPasswordError, "Veuillez entrer votre mot de passe actuel");
        }

        if (currentValid && newValid && confirmValid) {
            currentPassword = currentPasswordField.getText();
            newPassword = newPasswordField.getText();
            confirmPassword = confirmPasswordField.getText();
            isValid = true;
            closeDialog();
        }
    }

    @FXML
    private void handleCancel() {
        isValid = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) currentPasswordField.getScene().getWindow();
        stage.close();
    }

    // ===== GETTERS =====
    public boolean isValid() {
        return isValid;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}
