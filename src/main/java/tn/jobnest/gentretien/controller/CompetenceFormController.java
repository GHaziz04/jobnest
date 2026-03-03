package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.service.CompetenceService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CompetenceFormController {

    @FXML private TextField        nomField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private TextArea         descriptionField;
    @FXML private TextField        otpField;
    @FXML private ImageView        qrImage;
    @FXML private Button           btnSave;

    private final CompetenceService service = new CompetenceService();
    private Competence competence;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private GoogleAuthenticatorKey secretKey;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        categorieCombo.getItems().addAll("technique", "soft_skill", "langue", "autre");
        niveauCombo.getItems().addAll("debutant", "intermediaire", "avance", "expert");

        // Générer secret OTP pour nouveau ajout
        if (competence == null) {
            secretKey = gAuth.createCredentials();
            generateQrCode();
        }

        nomField.textProperty().addListener((o, a, b) -> validate());
        descriptionField.textProperty().addListener((o, a, b) -> validate());
        categorieCombo.valueProperty().addListener((o, a, b) -> validate());
        niveauCombo.valueProperty().addListener((o, a, b) -> validate());
        otpField.textProperty().addListener((o, a, b) -> validate());

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

        if (c.getOtpSecret() != null && !c.getOtpSecret().isEmpty()) {
            secretKey = new GoogleAuthenticatorKey.Builder(c.getOtpSecret()).build();
        } else {
            secretKey = gAuth.createCredentials();
            c.setOtpSecret(secretKey.getKey());
        }

        generateQrCode();
        validate();
    }

    // ================= SAVE =================
    @FXML
    private void save() {

        try {
            int code = Integer.parseInt(otpField.getText().trim());

            if (secretKey == null) {
                showError("Secret OTP introuvable ❌");
                return;
            }

            if (!gAuth.authorize(secretKey.getKey(), code)) {
                showError("Code Google Authenticator incorrect ❌");
                return;
            }

            if (competence == null) {
                // AJOUT
                competence = new Competence(
                        nomField.getText().trim(),
                        categorieCombo.getValue(),
                        descriptionField.getText().trim(),
                        niveauCombo.getValue(),
                        secretKey.getKey()
                );
                service.ajouter(competence);
                showSuccess("Compétence ajoutée ✅");
            } else {
                // MODIFICATION
                competence.setNom(nomField.getText().trim());
                competence.setCategorie(categorieCombo.getValue());
                competence.setNiveauRequis(niveauCombo.getValue());
                competence.setDescription(descriptionField.getText().trim());
                competence.setOtpSecret(secretKey.getKey());
                service.modifier(competence);
                showSuccess("Compétence modifiée ✅");
            }

            ((Stage) btnSave.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            showError("Code OTP invalide ❌");
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= QR CODE =================
    private void generateQrCode() {

        if (secretKey == null || qrImage == null) return;

        try {
            String issuer  = URLEncoder.encode("JobNest", StandardCharsets.UTF_8);
            String account = URLEncoder.encode("competence@jobnest.com", StandardCharsets.UTF_8);

            String otpAuthURL =
                    "otpauth://totp/" + issuer + ":" + account
                            + "?secret=" + secretKey.getKey()
                            + "&issuer=" + issuer
                            + "&algorithm=SHA1&digits=6&period=30";

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(otpAuthURL, BarcodeFormat.QR_CODE, 300, 300);

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            Image fxImage = SwingFXUtils.toFXImage(image, null);
            qrImage.setImage(fxImage);

        } catch (Exception e) {
            System.err.println("Erreur génération QR Code : " + e.getMessage());
        }
    }

    // ================= VALIDATION =================
    private void validate() {

        boolean nomOk  = nomField.getText() != null && nomField.getText().trim().length() >= 3;
        boolean descOk = descriptionField.getText() != null && descriptionField.getText().trim().length() >= 5;
        boolean catOk  = categorieCombo.getValue() != null;
        boolean nivOk  = niveauCombo.getValue() != null;
        boolean otpOk  = otpField.getText() != null && otpField.getText().matches("\\d{6}");

        style(nomField,          nomOk);
        style(descriptionField,  descOk);
        style(categorieCombo,    catOk);
        style(niveauCombo,       nivOk);
        style(otpField,          otpOk);

        if (btnSave != null)
            btnSave.setDisable(!(nomOk && descOk && catOk && nivOk && otpOk));
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