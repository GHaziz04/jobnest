package com.controller;

import com.utils.DBConnection;
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
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

    // ===== LOGIN =====
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button faceIdButton;
    @FXML private Label loginErrorLabel;

    // ===== REGISTER =====
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailRegField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordRegField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ImageView profileImage;
    @FXML private TextField adresseField;
    @FXML private CheckBox enrollFaceIdCheckbox;

    // ===== RESET PASSWORD =====
    @FXML private TextField resetEmailField;
    @FXML private TextField otpField;
    @FXML private PasswordField newPasswordField;

    // ===== VBOX CONTAINERS =====
    @FXML private VBox loginBox;
    @FXML private VBox registerBox;
    @FXML private VBox resetBox;

    // 🔴 Fichier image sélectionné
    private File selectedImageFile;

    // ===== INIT =====
    @FXML
    public void initialize() {
        // Initialiser le ComboBox des rôles
        roleBox.getItems().addAll(
                "Candidat",
                "Recruteur",
                "Formateur"
        );
        roleBox.getSelectionModel().selectFirst();

        // Focus auto sur email
        emailField.requestFocus();

        // Désactiver bouton si champs vides
        loginButton.disableProperty().bind(
                emailField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        // ENTER = LOGIN
        passwordField.setOnAction(e -> login());

        // Ajouter la validation des noms
        addNameValidation(prenomField);
        addNameValidation(nomField);
    }

    // ===== FACE ID AUTHENTICATION =====

    @FXML
    private void loginWithFaceID() {
        try {
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            boolean success = FaceIDController.showFaceIDDialog(currentStage);

            if (!success) {
                showLoginError("Authentification Face ID échouée");
            }
            // Si succès, la navigation vers Home est gérée dans FaceIDController

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur lors de l'authentification Face ID");
        }
    }

    // ===== NAVIGATION ENTRE LES VUES =====

    @FXML
    private void showResetPassword() {
        // Cacher les autres vues
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        registerBox.setVisible(false);
        registerBox.setManaged(false);

        // Afficher la vue de réinitialisation
        resetBox.setVisible(true);
        resetBox.setManaged(true);

        // Vider les champs
        resetEmailField.clear();
        otpField.clear();
        newPasswordField.clear();

        // Focus sur le champ email
        resetEmailField.requestFocus();
    }

    @FXML
    private void showRegister() {
        // Cacher les autres vues
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        resetBox.setVisible(false);
        resetBox.setManaged(false);

        // Afficher la vue d'inscription
        registerBox.setVisible(true);
        registerBox.setManaged(true);
    }

    @FXML
    private void showLogin() {
        // Cacher les autres vues
        registerBox.setVisible(false);
        registerBox.setManaged(false);
        resetBox.setVisible(false);
        resetBox.setManaged(false);

        // Afficher la vue de connexion
        loginBox.setVisible(true);
        loginBox.setManaged(true);

        // Réinitialiser le message d'erreur
        loginErrorLabel.setVisible(false);
        loginErrorLabel.setManaged(false);
    }

    // ===== RESET PASSWORD AVEC OTP =====

    private String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    @FXML
    private void sendOtp() {
        String email = resetEmailField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre email");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Erreur", "Email invalide");
            return;
        }

        String otp = generateOTP();

        try (Connection conn = DBConnection.getConnection()) {
            // Vérifier si l'email existe
            PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT id_user FROM users WHERE email = ?"
            );
            checkPs.setString(1, email);
            ResultSet checkRs = checkPs.executeQuery();

            if (!checkRs.next()) {
                showAlert("Erreur", "Aucun compte associé à cet email");
                return;
            }

            // Mettre à jour l'OTP dans la base de données
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET reset_otp = ?, otp_expiration = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE email = ?"
            );

            ps.setString(1, otp);
            ps.setString(2, email);
            ps.executeUpdate();

            // ✉️ ENVOI EMAIL
            EmailSender.sendOTP(email, otp);

            showAlert("Succès", "Un code OTP a été envoyé à votre email ✉️\nLe code expire dans 5 minutes.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'envoyer l'email. Vérifiez votre connexion.");
        }
    }

    @FXML
    private void resetPassword() {
        String email = resetEmailField.getText().trim();
        String otp = otpField.getText().trim();
        String newPassword = newPasswordField.getText();

        // Validation des champs
        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        if (newPassword.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Vérifier l'OTP
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_user FROM users WHERE email = ? AND reset_otp = ? AND otp_expiration > NOW()"
            );

            ps.setString(1, email);
            ps.setString(2, otp);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                showAlert("Erreur", "Code OTP invalide ou expiré");
                return;
            }

            // Mettre à jour le mot de passe
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET mot_de_passe = ?, reset_otp = NULL, otp_expiration = NULL WHERE email = ?"
            );

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
            // Bloquer chiffres et caractères spéciaux
            if (!newValue.matches("[A-Za-zÀ-ÿ ]*")) {
                field.setText(newValue.replaceAll("[^A-Za-zÀ-ÿ ]", ""));
                return;
            }

            // Première lettre en majuscule
            if (!newValue.isEmpty()) {
                String capitalized = newValue.substring(0, 1).toUpperCase() +
                        newValue.substring(1).toLowerCase();
                if (!newValue.equals(capitalized)) {
                    field.setText(capitalized);
                    field.positionCaret(capitalized.length());
                }
            }

            // Style visuel
            if (field.getText().length() < 2) {
                field.setStyle("-fx-border-color: red;");
            } else {
                field.setStyle("-fx-border-color: green;");
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // ===== LOGIN =====

    @FXML
    private void login() {
        String email = emailField.getText().trim();
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
            String sql = "SELECT * FROM users WHERE email = ? AND mot_de_passe = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // ✅ SESSION
                SessionManager.startSession(
                        rs.getInt("id_user"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("role")
                );

                // ✅ NAVIGATION
                navigateToHome();

            } else {
                showLoginError("Email ou mot de passe incorrect");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur de connexion à la base de données");
        }
    }

    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Home.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showLoginError("Erreur lors du chargement de la page d'accueil");
        }
    }

    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.setManaged(true);
    }

    // ===== IMAGE PROFIL =====

    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(profileImage.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            profileImage.setImage(new Image(file.toURI().toString()));
        }
    }

    // ===== REGISTER =====

    @FXML
    private void handleRegister() {
        // Validation du prénom et nom
        if (!prenomField.getText().matches("^[A-Za-zÀ-ÿ ]{2,}$")
                || !nomField.getText().matches("^[A-Za-zÀ-ÿ ]{2,}$")) {
            showAlert("Erreur", "Le prénom et le nom doivent contenir au moins 2 lettres");
            return;
        }

        // Validation de l'email
        if (!isValidEmail(emailRegField.getText())) {
            showAlert("Erreur", "Email invalide");
            return;
        }

        // Validation du téléphone
        if (!phoneField.getText().matches("\\d{8,15}")) {
            showAlert("Erreur", "Le téléphone doit contenir entre 8 et 15 chiffres");
            return;
        }

        // Vérifier que tous les champs sont remplis
        if (prenomField.getText().isEmpty()
                || nomField.getText().isEmpty()
                || emailRegField.getText().isEmpty()
                || passwordRegField.getText().isEmpty()
                || confirmPasswordField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
            return;
        }

        // Vérifier que les mots de passe correspondent
        if (!passwordRegField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas");
            return;
        }

        // Vérifier la longueur du mot de passe
        if (passwordRegField.getText().length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        // Vérifier qu'une image est sélectionnée
        if (selectedImageFile == null) {
            showAlert("Erreur", "Veuillez sélectionner une photo de profil");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                showAlert("Erreur", "Impossible de se connecter à la base de données");
                return;
            }

            // ✅ Vérifier si l'email existe déjà
            PreparedStatement checkEmail =
                    conn.prepareStatement("SELECT id_user FROM users WHERE email = ?");
            checkEmail.setString(1, emailRegField.getText());

            ResultSet rsCheck = checkEmail.executeQuery();
            if (rsCheck.next()) {
                showAlert("Erreur", "Cet email est déjà utilisé");
                return;
            }

            // Copier l'image dans un dossier permanent
            String userImagesDir = "user_images/";
            File imageDir = new File(userImagesDir);
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            // Créer un nom de fichier unique
            String imageExtension = selectedImageFile.getName().substring(
                    selectedImageFile.getName().lastIndexOf(".")
            );
            String newImageName = "user_" + System.currentTimeMillis() + imageExtension;
            String imagePath = userImagesDir + newImageName;

            // Copier le fichier
            File destFile = new File(imagePath);
            java.nio.file.Files.copy(
                    selectedImageFile.toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("📁 Image copiée vers: " + imagePath);

            // Insérer l'utilisateur avec le chemin de l'image
            String sqlUser = """
                INSERT INTO users
                (prenom, nom, email, telephone, mot_de_passe, role, photo_profil, adresse, face_id_enrolled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement psUser =
                    conn.prepareStatement(sqlUser, PreparedStatement.RETURN_GENERATED_KEYS);

            psUser.setString(1, prenomField.getText());
            psUser.setString(2, nomField.getText());
            psUser.setString(3, emailRegField.getText());
            psUser.setString(4, phoneField.getText());
            psUser.setString(5, passwordRegField.getText());
            psUser.setString(6, roleBox.getValue());
            psUser.setString(7, imagePath);  // Stocker le chemin au lieu des bytes
            psUser.setString(8, adresseField.getText());
            psUser.setBoolean(9, enrollFaceIdCheckbox != null && enrollFaceIdCheckbox.isSelected());

            psUser.executeUpdate();

            ResultSet rs = psUser.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1);
                String role = roleBox.getValue();

                // Insérer dans la table spécifique au rôle
                if (role.equalsIgnoreCase("Candidat")) {
                    PreparedStatement psCandidat =
                            conn.prepareStatement("INSERT INTO candidat (id_user) VALUES (?)");
                    psCandidat.setInt(1, userId);
                    psCandidat.executeUpdate();
                } else if (role.equalsIgnoreCase("Recruteur")) {
                    PreparedStatement psRecruteur =
                            conn.prepareStatement("INSERT INTO recruteur (id_user) VALUES (?)");
                    psRecruteur.setInt(1, userId);
                    psRecruteur.executeUpdate();
                } else if (role.equalsIgnoreCase("Formateur")) {
                    PreparedStatement psFormateur =
                            conn.prepareStatement("INSERT INTO formateur (id_user) VALUES (?)");
                    psFormateur.setInt(1, userId);
                    psFormateur.executeUpdate();
                }

                // 🎯 NOUVEAU : Si l'utilisateur souhaite enregistrer son Face ID depuis la photo
                if (enrollFaceIdCheckbox != null && enrollFaceIdCheckbox.isSelected()) {
                    System.out.println("🔐 Configuration Face ID depuis la photo de profil...");

                    boolean faceIdSuccess = FaceIDAuthenticator.enrollFaceFromImage(
                            userId,
                            selectedImageFile.getAbsolutePath()
                    );

                    if (faceIdSuccess) {
                        showAlert("Succès",
                                "Compte créé avec succès ! 🎉\n\n" +
                                        "✅ Face ID configuré depuis votre photo\n" +
                                        "Vous pouvez maintenant vous connecter avec votre visage.");
                    } else {
                        showAlert("Avertissement",
                                "Compte créé avec succès ! 🎉\n\n" +
                                        "⚠️ Face ID n'a pas pu être configuré\n" +
                                        "Aucun visage détecté dans l'image.\n" +
                                        "Vous pouvez le configurer plus tard dans les paramètres.");
                    }
                } else {
                    showAlert("Succès",
                            "Compte créé avec succès ! 🎉\n" +
                                    "Vous pouvez maintenant vous connecter.");
                }
            }

            // Réinitialiser les champs
            clearRegisterFields();

            // Retourner à la page de connexion
            showLogin();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    private void clearRegisterFields() {
        prenomField.clear();
        nomField.clear();
        emailRegField.clear();
        phoneField.clear();
        passwordRegField.clear();
        confirmPasswordField.clear();
        adresseField.clear();
        if (enrollFaceIdCheckbox != null) {
            enrollFaceIdCheckbox.setSelected(false);
        }
        selectedImageFile = null;

        try {
            profileImage.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
        } catch (Exception e) {
            // Image par défaut non trouvée, ignorer
        }

        roleBox.getSelectionModel().selectFirst();
    }

    // ===== ALERT =====

    private void showAlert(String title, String message) {
        Alert alert = new Alert(
                title.equals("Erreur") || title.equals("Avertissement") ?
                        Alert.AlertType.WARNING : Alert.AlertType.INFORMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}