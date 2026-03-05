package com.controller;

import com.utils.TwoFactorAuth;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TwoFAVerifyController {

    @FXML private TextField d1, d2, d3, d4, d5, d6;
    @FXML private Label     errorLabel;
    @FXML private Button    verifyBtn;

    private String  secret;
    private boolean verified = false;

    @FXML
    public void initialize() {
        // Auto-focus et navigation automatique entre les champs
        setupDigitField(d1, null, d2);
        setupDigitField(d2, d1,   d3);
        setupDigitField(d3, d2,   d4);
        setupDigitField(d4, d3,   d5);
        setupDigitField(d5, d4,   d6);
        setupDigitField(d6, d5,   null);

        javafx.application.Platform.runLater(() -> d1.requestFocus());
    }

    private void setupDigitField(TextField current, TextField prev, TextField next) {
        current.textProperty().addListener((obs, oldVal, newVal) -> {
            // Accepter uniquement les chiffres
            if (!newVal.matches("\\d*")) {
                current.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            // Max 1 chiffre
            if (newVal.length() > 1) {
                current.setText(newVal.substring(0, 1));
            }
            // Passer au champ suivant automatiquement
            if (newVal.length() == 1 && next != null) {
                next.requestFocus();
            }
            // Auto-vérifier quand les 6 chiffres sont remplis
            if (getCode().length() == 6) {
                verify();
            }
        });

        // Backspace → revenir au champ précédent
        current.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE
                    && current.getText().isEmpty() && prev != null) {
                prev.requestFocus();
            }
        });

        // Gérer le paste (coller un code complet)
        current.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.V) {
                javafx.application.Platform.runLater(this::handlePaste);
            }
        });
    }

    private void handlePaste() {
        String clipboard = javafx.scene.input.Clipboard.getSystemClipboard().getString();
        if (clipboard != null && clipboard.replaceAll("\\s", "").matches("\\d{6}")) {
            String digits = clipboard.replaceAll("\\s", "");
            d1.setText(String.valueOf(digits.charAt(0)));
            d2.setText(String.valueOf(digits.charAt(1)));
            d3.setText(String.valueOf(digits.charAt(2)));
            d4.setText(String.valueOf(digits.charAt(3)));
            d5.setText(String.valueOf(digits.charAt(4)));
            d6.setText(String.valueOf(digits.charAt(5)));
            verify();
        }
    }

    private String getCode() {
        return d1.getText() + d2.getText() + d3.getText()
                + d4.getText() + d5.getText() + d6.getText();
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @FXML
    private void verify() {
        String code = getCode();
        if (code.length() != 6) {
            showError("Entrez les 6 chiffres du code");
            return;
        }

        try {
            int codeInt = Integer.parseInt(code);
            if (TwoFactorAuth.verifyCode(secret, codeInt)) {
                verified = true;
                ((Stage) d1.getScene().getWindow()).close();
            } else {
                showError("Code incorrect — vérifiez l'heure de votre téléphone");
                clearFields();
            }
        } catch (Exception e) {
            showError("Erreur de vérification");
        }
    }

    private void clearFields() {
        d1.clear(); d2.clear(); d3.clear();
        d4.clear(); d5.clear(); d6.clear();
        d1.requestFocus();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠️ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Shake animation
        javafx.animation.TranslateTransition shake =
                new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(60), errorLabel);
        shake.setFromX(-8); shake.setToX(8);
        shake.setCycleCount(4); shake.setAutoReverse(true);
        shake.play();
    }

    public boolean isVerified() { return verified; }
}