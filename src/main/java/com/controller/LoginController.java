package com.controller;

import com.utils.DBConnection;
import com.utils.AccountVerifier;
import com.utils.EmailSender;
import com.utils.SessionManager;
import com.utils.FaceIDAuthenticator;
import com.utils.StageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.sql.ResultSet;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

    // ===== LOGIN =====
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Button        faceIdButton;
    @FXML private Label         loginErrorLabel;

    // ===== REGISTER =====
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField        prenomField;
    @FXML private TextField        nomField;
    @FXML private TextField        emailRegField;
    @FXML private TextField        phoneField;
    @FXML private PasswordField    passwordRegField;
    @FXML private PasswordField    confirmPasswordField;
    @FXML private ImageView        profileImage;
    @FXML private TextField        adresseField;
    @FXML private CheckBox         enrollFaceIdCheckbox;

    // ===== RESET PASSWORD =====
    @FXML private TextField     resetEmailField;
    @FXML private TextField     otpField;
    @FXML private PasswordField newPasswordField;

    // ===== VBOX CONTAINERS =====
    @FXML private VBox loginBox;
    @FXML private VBox registerBox;
    @FXML private VBox resetBox;

    // ===== VERIFICATION COMPTE =====
    @FXML private VBox    verifyBox;
    @FXML private TextField tokenField;
    private File selectedImageFile;

    // ===== INIT =====
    @FXML
    public void initialize() {
        roleBox.getItems().addAll("Candidat", "Recruteur", "Formateur");
        roleBox.getSelectionModel().selectFirst();

        emailField.requestFocus();

        loginButton.disableProperty().bind(
                emailField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        passwordField.setOnAction(e -> login());

        addNameValidation(prenomField);
        addNameValidation(nomField);
    }

    // ===== FACE ID =====
    @FXML
    private void loginWithFaceID() {
        try {
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            boolean success = FaceIDController.showFaceIDDialog(currentStage);
            if (!success) showLoginError("Authentification Face ID échouée");
        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur lors de l'authentification Face ID");
        }
    }

    // ===== NAVIGATION =====
    @FXML
    private void showResetPassword() {
        loginBox.setVisible(false);    loginBox.setManaged(false);
        registerBox.setVisible(false); registerBox.setManaged(false);
        resetBox.setVisible(true);     resetBox.setManaged(true);
        resetEmailField.clear();
        otpField.clear();
        newPasswordField.clear();
        resetEmailField.requestFocus();
    }

    @FXML
    private void showRegister() {
        loginBox.setVisible(false);    loginBox.setManaged(false);
        resetBox.setVisible(false);    resetBox.setManaged(false);
        registerBox.setVisible(true);  registerBox.setManaged(true);
    }

    @FXML
    private void showLogin() {
        registerBox.setVisible(false); registerBox.setManaged(false);
        resetBox.setVisible(false);    resetBox.setManaged(false);
        loginBox.setVisible(true);     loginBox.setManaged(true);
        loginErrorLabel.setVisible(false);
        loginErrorLabel.setManaged(false);
    }

    // ===== LOGIN — avec vérification statut bloqué =====
    @FXML
    private void login() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showLoginError("Email invalide");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // ── Récupérer l'utilisateur avec son statut ──────────────
            String sql = "SELECT * FROM users WHERE email = ? AND mot_de_passe = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                showLoginError("Email ou mot de passe incorrect");
                return;
            }

            // ── Vérification du statut du compte ────────────────────
            String statut = null;
            try { statut = rs.getString("statut"); } catch (Exception ignored) {}

            if ("inactif".equalsIgnoreCase(statut)) {
                showLoginError("⚠️ Votre compte n'est pas encore vérifié.\nVérifiez votre boîte email.");
                return;
            }

            if ("bloque".equalsIgnoreCase(statut)) {
                showLoginError("❌ Votre compte a été suspendu.\nContactez l'administrateur.");
                return;
            }
            // ── Vérification 2FA si activé ───────────────────────────────
            boolean twoFaEnabled = rs.getBoolean("two_fa_enabled");
            if (twoFaEnabled) {
                String secret = rs.getString("totp_secret");
                boolean twoFaOk = showTwoFAVerifyDialog(secret);
                if (!twoFaOk) {
                    showLoginError("❌ Code 2FA invalide. Connexion refusée.");
                    return;
                }
            }

            // ── Connexion réussie ────────────────────────────────────
            int    userId = rs.getInt("id_user");
            String prenom = rs.getString("prenom");
            String nom    = rs.getString("nom");
            String role   = rs.getString("role");

            // Mettre à jour last_login (colonne optionnelle — pas d'erreur si absente)
            try {
                PreparedStatement psUpdate = conn.prepareStatement(
                        "UPDATE users SET last_login = NOW() WHERE id_user = ?");
                psUpdate.setInt(1, userId);
                psUpdate.executeUpdate();
            } catch (Exception ignored) {}

            // Enregistrer la session
            SessionManager.startSession(userId, prenom, nom, email, role);

            // Naviguer vers l'accueil
            navigateToHome();

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur de connexion à la base de données");
        }
    }

    private boolean showTwoFAVerifyDialog(String secret) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/TwoFAVerifyDialog.fxml"));
            Parent root = loader.load();
            root.getStylesheets().add(
                    getClass().getResource("/css/twofa-dialog.css").toExternalForm());

            TwoFAVerifyController controller = loader.getController();
            controller.setSecret(secret);

            Stage stage = new Stage();
            stage.setTitle("🔐 Vérification 2FA - JobNest");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(emailField.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.isVerified();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur lors du chargement de la page d'accueil");
        }
    }

    // ===== RESET PASSWORD AVEC OTP =====
    private String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    @FXML
    private void sendOtp() {
        String email = resetEmailField.getText().trim();

        if (email.isEmpty())         { showAlert("Erreur", "Veuillez entrer votre email"); return; }
        if (!isValidEmail(email))    { showAlert("Erreur", "Email invalide"); return; }

        String otp = generateOTP();

        try (Connection conn = DBConnection.getConnection()) {
            // Vérifier si l'email existe
            PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT id_user FROM users WHERE email = ?");
            checkPs.setString(1, email);
            ResultSet checkRs = checkPs.executeQuery();

            if (!checkRs.next()) {
                showAlert("Erreur", "Aucun compte associé à cet email");
                return;
            }

            // Sauvegarder l'OTP avec expiration 5 min
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET reset_otp = ?, otp_expiration = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE email = ?");
            ps.setString(1, otp);
            ps.setString(2, email);
            ps.executeUpdate();

            // Envoi de l'email OTP (utilise ton EmailSender existant)
            EmailSender.sendOTP(email, otp);

            showAlert("Succès", "Un code OTP a été envoyé à votre email ✉️\nLe code expire dans 5 minutes.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'envoyer l'email. Vérifiez votre connexion.");
        }
    }

    @FXML
    private void resetPassword() {
        String email       = resetEmailField.getText().trim();
        String otp         = otpField.getText().trim();
        String newPassword = newPasswordField.getText();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs"); return;
        }
        if (newPassword.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères"); return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Vérifier OTP valide et non expiré
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_user FROM users WHERE email = ? AND reset_otp = ? AND otp_expiration > NOW()");
            ps.setString(1, email);
            ps.setString(2, otp);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                showAlert("Erreur", "Code OTP invalide ou expiré"); return;
            }

            // Mettre à jour le mot de passe et effacer l'OTP
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET mot_de_passe = ?, reset_otp = NULL, otp_expiration = NULL WHERE email = ?");
            update.setString(1, newPassword);
            update.setString(2, email);
            update.executeUpdate();

            showAlert("Succès", "Mot de passe réinitialisé avec succès 🎉");
            showLogin();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la réinitialisation du mot de passe");
        }
    }

    // ===== VALIDATION =====
    private void addNameValidation(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[A-Za-zÀ-ÿ ]*")) {
                field.setText(newValue.replaceAll("[^A-Za-zÀ-ÿ ]", ""));
                return;
            }
            if (!newValue.isEmpty()) {
                String capitalized = newValue.substring(0,1).toUpperCase() + newValue.substring(1).toLowerCase();
                if (!newValue.equals(capitalized)) {
                    field.setText(capitalized);
                    field.positionCaret(capitalized.length());
                }
            }
            field.setStyle(field.getText().length() < 2 ? "-fx-border-color:red;" : "-fx-border-color:green;");
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // ===== IMAGE PROFIL =====
    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(profileImage.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            profileImage.setImage(new Image(file.toURI().toString()));
        }
    }

    // ===== REGISTER =====
    @FXML
    private void handleRegister() {
        // ===== VALIDATIONS =====
        if (!prenomField.getText().matches("^[A-Za-zÀ-ÿ ]{2,}$")
                || !nomField.getText().matches("^[A-Za-zÀ-ÿ ]{2,}$")) {
            showAlert("Erreur", "Le prénom et le nom doivent contenir au moins 2 lettres"); return;
        }
        if (!isValidEmail(emailRegField.getText())) {
            showAlert("Erreur", "Email invalide"); return;
        }
        if (!phoneField.getText().matches("\\d{8,15}")) {
            showAlert("Erreur", "Le téléphone doit contenir entre 8 et 15 chiffres"); return;
        }
        if (prenomField.getText().isEmpty() || nomField.getText().isEmpty()
                || emailRegField.getText().isEmpty() || passwordRegField.getText().isEmpty()
                || confirmPasswordField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires"); return;
        }
        if (!passwordRegField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas"); return;
        }
        if (passwordRegField.getText().length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères"); return;
        }
        if (selectedImageFile == null) {
            showAlert("Erreur", "Veuillez sélectionner une photo de profil"); return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) { showAlert("Erreur", "Impossible de se connecter à la base de données"); return; }

            // ── Vérifier si email déjà utilisé ──────────────────────────
            PreparedStatement checkEmail = conn.prepareStatement(
                    "SELECT id_user FROM users WHERE email = ?");
            checkEmail.setString(1, emailRegField.getText());
            if (checkEmail.executeQuery().next()) {
                showAlert("Erreur", "Cet email est déjà utilisé"); return;
            }

            // ── Copier l'image dans le dossier permanent ─────────────────
            String userImagesDir = "user_images/";
            new File(userImagesDir).mkdirs();
            String ext          = selectedImageFile.getName().substring(
                    selectedImageFile.getName().lastIndexOf("."));
            String newImageName = "user_" + System.currentTimeMillis() + ext;
            String imagePath    = userImagesDir + newImageName;
            java.nio.file.Files.copy(selectedImageFile.toPath(), new File(imagePath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.out.println("📁 Image copiée vers : " + imagePath);

            // ── Générer le token de vérification ─────────────────────────
            String verificationToken = java.util.UUID.randomUUID().toString();

            // ── Insérer l'utilisateur avec statut 'inactif' ──────────────
            String sqlUser = """
            INSERT INTO users
            (prenom, nom, email, telephone, mot_de_passe, role, photo_profil, adresse,
             face_id_enrolled, statut, verification_token, token_expiration)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'inactif', ?, DATE_ADD(NOW(), INTERVAL 24 HOUR))
            """;
            PreparedStatement psUser = conn.prepareStatement(sqlUser, PreparedStatement.RETURN_GENERATED_KEYS);
            psUser.setString(1, prenomField.getText());
            psUser.setString(2, nomField.getText());
            psUser.setString(3, emailRegField.getText());
            psUser.setString(4, phoneField.getText());
            psUser.setString(5, passwordRegField.getText());
            psUser.setString(6, roleBox.getValue());
            psUser.setString(7, imagePath);
            psUser.setString(8, adresseField.getText());
            psUser.setBoolean(9, enrollFaceIdCheckbox != null && enrollFaceIdCheckbox.isSelected());
            psUser.setString(10, verificationToken);
            psUser.executeUpdate();

            ResultSet rs = psUser.getGeneratedKeys();
            if (rs.next()) {
                int    userId = rs.getInt(1);
                String role   = roleBox.getValue();

                // ── Insérer dans la table spécifique au rôle ─────────────
                String roleTable = switch (role.toLowerCase()) {
                    case "recruteur" -> "recruteur";
                    case "formateur" -> "formateur";
                    default          -> "candidat";
                };
                PreparedStatement psRole = conn.prepareStatement(
                        "INSERT INTO " + roleTable + " (id_user) VALUES (?)");
                psRole.setInt(1, userId);
                psRole.executeUpdate();

                // ── Envoi de l'email de vérification ─────────────────────
                boolean emailSent = false;
                try {
                    EmailSender.sendVerificationEmail(
                            emailRegField.getText(),
                            prenomField.getText(),
                            verificationToken
                    );
                    emailSent = true;
                    System.out.println("✅ Email de vérification envoyé à : " + emailRegField.getText());
                } catch (Exception mailEx) {
                    System.err.println("⚠️ Impossible d'envoyer l'email : " + mailEx.getMessage());
                }

                // ── Face ID optionnel ─────────────────────────────────────
                if (enrollFaceIdCheckbox != null && enrollFaceIdCheckbox.isSelected()) {
                    boolean faceOk = FaceIDAuthenticator.enrollFaceFromImage(
                            userId, selectedImageFile.getAbsolutePath());

                    showAlert("Succès", faceOk
                            ? "Compte créé avec succès ! 🎉\n\n"
                            + "✅ Face ID configuré depuis votre photo.\n\n"
                            + (emailSent
                            ? "📧 Un email de vérification a été envoyé à :\n" + emailRegField.getText()
                            + "\nVeuillez vérifier votre boîte mail pour activer votre compte."
                            : "⚠️ L'email de vérification n'a pas pu être envoyé.\nContactez le support.")
                            : "Compte créé avec succès ! 🎉\n\n"
                            + "⚠️ Face ID n'a pas pu être configuré (aucun visage détecté).\n\n"
                            + (emailSent
                            ? "📧 Un email de vérification a été envoyé à :\n" + emailRegField.getText()
                            + "\nVeuillez vérifier votre boîte mail pour activer votre compte."
                            : "⚠️ L'email de vérification n'a pas pu être envoyé.\nContactez le support."));
                } else {
                    showAlert("Succès",
                            "Compte créé avec succès ! 🎉\n\n"
                                    + (emailSent
                                    ? "📧 Un email de vérification a été envoyé à :\n" + emailRegField.getText()
                                    + "\n\nVeuillez cliquer sur le lien dans l'email\npour activer votre compte."
                                    : "⚠️ L'email de vérification n'a pas pu être envoyé.\nContactez le support."));
                }
            }

            clearRegisterFields();
            showLogin();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    @FXML
    private void verifyAccount() {
        String token = tokenField.getText().trim();

        if (token.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre token de vérification");
            return;
        }

        boolean ok = AccountVerifier.verifyAccount(token);

        if (ok) {
            showAlert("Succès", "✅ Votre compte est maintenant actif !\nVous pouvez vous connecter.");
            tokenField.clear();
            showLogin();
        } else {
            showAlert("Erreur", "❌ Token invalide ou expiré.\nVérifiez votre email ou demandez un nouveau lien.");
        }
    }

    @FXML
    private void showVerify() {
        loginBox.setVisible(false);    loginBox.setManaged(false);
        registerBox.setVisible(false); registerBox.setManaged(false);
        resetBox.setVisible(false);    resetBox.setManaged(false);
        verifyBox.setVisible(true);    verifyBox.setManaged(true);
        tokenField.requestFocus();
    }

    private void clearRegisterFields() {
        prenomField.clear(); nomField.clear(); emailRegField.clear();
        phoneField.clear(); passwordRegField.clear(); confirmPasswordField.clear(); adresseField.clear();
        if (enrollFaceIdCheckbox != null) enrollFaceIdCheckbox.setSelected(false);
        selectedImageFile = null;
        try {
            profileImage.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
        } catch (Exception ignored) {}
        roleBox.getSelectionModel().selectFirst();
    }

    // ===== ALERT =====
    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.setManaged(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(
                title.equals("Erreur") || title.equals("Avertissement")
                        ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}