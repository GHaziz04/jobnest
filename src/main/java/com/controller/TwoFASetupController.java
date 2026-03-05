package com.controller;

import com.utils.SessionManager;
import com.utils.TwoFactorAuth;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

// QR Code generation
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class TwoFASetupController {

    @FXML private ImageView qrImageView;
    @FXML private Label     secretLabel;
    @FXML private TextField codeField;
    @FXML private Label     errorLabel;

    private String  secretKey;
    private boolean activated = false;

    @FXML
    public void initialize() {
        // Générer une nouvelle clé secrète
        GoogleAuthenticatorKey credentials = TwoFactorAuth.generateSecret();
        secretKey = credentials.getKey();

        // Afficher la clé secrète
        secretLabel.setText(formatSecret(secretKey));

        // Générer et afficher le QR code
        generateQRCode();

        // Limiter le champ à 6 chiffres
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))   codeField.setText(newVal.replaceAll("[^\\d]", ""));
            if (newVal.length() > 6)       codeField.setText(newVal.substring(0, 6));
        });
    }

    private void generateQRCode() {
        try {
            String email  = SessionManager.getInstance().getEmail();
            String otpUrl = TwoFactorAuth.getQRCodeUrl(email, secretKey);

            QRCodeWriter writer    = new QRCodeWriter();
            BitMatrix    bitMatrix = writer.encode(otpUrl, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            Image qrImage = new Image(new ByteArrayInputStream(baos.toByteArray()));
            qrImageView.setImage(qrImage);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de générer le QR code");
        }
    }

    @FXML
    private void verifyAndEnable() {
        String codeText = codeField.getText().trim();

        if (codeText.length() != 6) {
            showError("Le code doit contenir exactement 6 chiffres");
            return;
        }

        try {
            int code = Integer.parseInt(codeText);
            if (TwoFactorAuth.verifyCode(secretKey, code)) {
                // Code correct → sauvegarder en DB
                boolean saved = TwoFactorAuth.enableTwoFA(
                        SessionManager.getInstance().getId(), secretKey);

                if (saved) {
                    activated = true;
                    showSuccess();
                } else {
                    showError("Erreur lors de la sauvegarde. Réessayez.");
                }
            } else {
                showError("❌ Code incorrect. Vérifiez l'heure de votre téléphone.");
                codeField.clear();
                codeField.requestFocus();
            }
        } catch (NumberFormatException e) {
            showError("Entrez uniquement des chiffres");
        }
    }

    private void showSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("2FA Activé ✅");
        alert.setHeaderText(null);
        alert.setContentText(
                "🎉 Double authentification activée avec succès !\n\n" +
                        "À chaque connexion, vous devrez entrer\n" +
                        "le code affiché dans Google Authenticator."
        );
        alert.showAndWait();
        ((Stage) codeField.getScene().getWindow()).close();
    }

    @FXML
    private void copySecret() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(secretKey);
        clipboard.setContent(content);

        secretLabel.setText("✅ Copié !");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> secretLabel.setText(formatSecret(secretKey)));
        pause.play();
    }

    @FXML
    private void cancel() {
        ((Stage) codeField.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠️ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private String formatSecret(String secret) {
        // Formater par groupes de 4 : ABCD EFGH IJKL ...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(secret.charAt(i));
        }
        return sb.toString();
    }

    public boolean isActivated() { return activated; }
}