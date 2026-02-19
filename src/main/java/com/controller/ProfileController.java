package com.controller;

import com.utils.DBConnection;
import com.utils.SessionManager;
import com.utils.StageUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileController {

    // ===== HEADER =====
    @FXML private Label welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;

    // ===== FORM FIELDS =====
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField adresseField;

    // ===== BUTTONS =====
    @FXML private Button editButton;
    @FXML private HBox actionButtons;

    // ===== STATISTICS =====
    @FXML private VBox statsSection;
    @FXML private Label stat1Value;
    @FXML private Label stat1Label;
    @FXML private Label stat2Value;
    @FXML private Label stat2Label;
    @FXML private Label stat3Value;
    @FXML private Label stat3Label;

    private String imagePath;
    private boolean editMode = false;

    // ===== INIT =====
    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        welcomeLabel.setText(SessionManager.getInstance().getPrenom() + " 👋");
        loadUserProfile();
        loadStatistics();
    }

    // ===== LOAD USER PROFILE =====
    private void loadUserProfile() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE id_user = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, SessionManager.getInstance().getId());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Header
                fullNameLabel.setText(rs.getString("prenom") + " " + rs.getString("nom"));
                roleLabel.setText(rs.getString("role"));
                emailLabel.setText(rs.getString("email"));

                // Form fields
                prenomField.setText(rs.getString("prenom"));
                nomField.setText(rs.getString("nom"));
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("telephone"));
                adresseField.setText(rs.getString("adresse"));

                // Photo de profil
                String photoPath = rs.getString("photo_profil");
                if (photoPath != null && !photoPath.isEmpty()) {
                    File imageFile = new File(photoPath);
                    if (imageFile.exists()) {
                        imagePath = photoPath;
                        profileImageView.setImage(new Image(imageFile.toURI().toString()));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le profil");
        }
    }

    // ===== LOAD STATISTICS =====
    private void loadStatistics() {
        String role = SessionManager.getInstance().getRole();

        try (Connection conn = DBConnection.getConnection()) {
            if (role.equalsIgnoreCase("Candidat")) {
                loadCandidatStats(conn);
            } else if (role.equalsIgnoreCase("Recruteur")) {
                loadRecruteurStats(conn);
            } else if (role.equalsIgnoreCase("Formateur")) {
                loadFormateurStats(conn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCandidatStats(Connection conn) throws Exception {
        int userId = SessionManager.getInstance().getId();

        // Nombre de candidatures
        PreparedStatement ps1 = conn.prepareStatement(
                "SELECT COUNT(*) FROM candidature c " +
                        "JOIN candidat ca ON c.id_candidat = ca.id_candidat " +
                        "WHERE ca.id_user = ?"
        );
        ps1.setInt(1, userId);
        ResultSet rs1 = ps1.executeQuery();
        if (rs1.next()) {
            stat1Value.setText(String.valueOf(rs1.getInt(1)));
            stat1Label.setText("Candidatures");
        }

        // Nombre d'entretiens
        PreparedStatement ps2 = conn.prepareStatement(
                "SELECT COUNT(*) FROM entretien e " +
                        "JOIN candidature c ON e.id_candidature = c.id_candidature " +
                        "JOIN candidat ca ON c.id_candidat = ca.id_candidat " +
                        "WHERE ca.id_user = ?"
        );
        ps2.setInt(1, userId);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next()) {
            stat2Value.setText(String.valueOf(rs2.getInt(1)));
            stat2Label.setText("Entretiens");
        }

        // Formations suivies
        stat3Value.setText("0");
        stat3Label.setText("Formations");
    }

    private void loadRecruteurStats(Connection conn) throws Exception {
        int userId = SessionManager.getInstance().getId();

        // Nombre d'offres publiées
        PreparedStatement ps1 = conn.prepareStatement(
                "SELECT COUNT(*) FROM offre o " +
                        "JOIN recruteur r ON o.id_recruteur = r.id_recruteur " +
                        "WHERE r.id_user = ?"
        );
        ps1.setInt(1, userId);
        ResultSet rs1 = ps1.executeQuery();
        if (rs1.next()) {
            stat1Value.setText(String.valueOf(rs1.getInt(1)));
            stat1Label.setText("Offres publiées");
        }

        // Nombre de candidatures reçues
        PreparedStatement ps2 = conn.prepareStatement(
                "SELECT COUNT(*) FROM candidature c " +
                        "JOIN offre o ON c.id_offre = o.id_offre " +
                        "JOIN recruteur r ON o.id_recruteur = r.id_recruteur " +
                        "WHERE r.id_user = ?"
        );
        ps2.setInt(1, userId);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next()) {
            stat2Value.setText(String.valueOf(rs2.getInt(1)));
            stat2Label.setText("Candidatures");
        }

        // Nombre d'entretiens planifiés
        PreparedStatement ps3 = conn.prepareStatement(
                "SELECT COUNT(*) FROM entretien e " +
                        "JOIN candidature c ON e.id_candidature = c.id_candidature " +
                        "JOIN offre o ON c.id_offre = o.id_offre " +
                        "JOIN recruteur r ON o.id_recruteur = r.id_recruteur " +
                        "WHERE r.id_user = ?"
        );
        ps3.setInt(1, userId);
        ResultSet rs3 = ps3.executeQuery();
        if (rs3.next()) {
            stat3Value.setText(String.valueOf(rs3.getInt(1)));
            stat3Label.setText("Entretiens");
        }
    }

    private void loadFormateurStats(Connection conn) throws Exception {
        // Pour formateur - à adapter selon votre schéma
        stat1Value.setText("0");
        stat1Label.setText("Formations");
        stat2Value.setText("0");
        stat2Label.setText("Participants");
        stat3Value.setText("0");
        stat3Label.setText("Cours");
    }

    // ===== TOGGLE EDIT MODE =====
    @FXML
    private void toggleEditMode() {
        editMode = !editMode;

        prenomField.setEditable(editMode);
        nomField.setEditable(editMode);
        emailField.setEditable(editMode);
        phoneField.setEditable(editMode);
        adresseField.setEditable(editMode);

        actionButtons.setVisible(editMode);
        actionButtons.setManaged(editMode);

        editButton.setText(editMode ? "Mode lecture" : "Modifier");
    }

    // ===== SAVE PROFILE =====
    @FXML
    private void saveProfile() {
        if (!validateFields()) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET prenom = ?, nom = ?, email = ?, " +
                    "telephone = ?, adresse = ?, photo_profil = ? WHERE id_user = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, prenomField.getText().trim());
            ps.setString(2, nomField.getText().trim());
            ps.setString(3, emailField.getText().trim());
            ps.setString(4, phoneField.getText().trim());
            ps.setString(5, adresseField.getText().trim());
            ps.setString(6, imagePath);
            ps.setInt(7, SessionManager.getInstance().getId());

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                // Mettre à jour la session
                SessionManager.getInstance().setPrenom(prenomField.getText().trim());
                SessionManager.getInstance().setNom(nomField.getText().trim());
                SessionManager.getInstance().setEmail(emailField.getText().trim());

                showAlert("Succès", "Profil mis à jour avec succès 🎉");
                toggleEditMode();
                loadUserProfile(); // Recharger les données
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la mise à jour du profil");
        }
    }

    // ===== CANCEL EDIT =====
    @FXML
    private void cancelEdit() {
        toggleEditMode();
        loadUserProfile(); // Restaurer les valeurs originales
    }

    // ===== CHANGE PROFILE IMAGE =====
    @FXML
    private void changeProfileImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            imagePath = file.getAbsolutePath();
            profileImageView.setImage(new Image(file.toURI().toString()));

            // Sauvegarder immédiatement la photo
            saveProfileImage();
        }
    }

    private void saveProfileImage() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET photo_profil = ? WHERE id_user = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, imagePath);
            ps.setInt(2, SessionManager.getInstance().getUserId());
            ps.executeUpdate();

            showAlert("Succès", "Photo de profil mise à jour ✅");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la mise à jour de la photo");
        }
    }

    // ===== CHANGE PASSWORD =====
    // ===== CHANGE PASSWORD =====
    @FXML
    private void changePassword() {
        try {
            // Charger le dialogue personnalisé
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ChangePasswordDialog.fxml")
            );
            Parent root = loader.load();

            // Ajouter le CSS
            root.getStylesheets().add(
                    getClass().getResource("/css/password-dialog.css").toExternalForm()
            );

            // Créer une nouvelle fenêtre
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Changer le mot de passe");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            // Récupérer le contrôleur
            ChangePasswordDialogController controller = loader.getController();

            // Afficher et attendre
            dialogStage.showAndWait();

            // Vérifier si validé
            if (controller.isValid()) {
                updatePassword(
                        controller.getCurrentPassword(),
                        controller.getNewPassword(),
                        controller.getConfirmPassword()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le dialogue de changement de mot de passe");
        }
    }

    private void updatePassword(String current, String newPass, String confirm) {
        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs");
            return;
        }

        if (!newPass.equals(confirm)) {
            showAlert("Erreur", "Les mots de passe ne correspondent pas");
            return;
        }

        if (newPass.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Vérifier le mot de passe actuel
            String checkSql = "SELECT mot_de_passe FROM users WHERE id_user = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, SessionManager.getInstance().getUserId());
            ResultSet rs = checkPs.executeQuery();

            if (rs.next() && rs.getString("mot_de_passe").equals(current)) {
                // Mettre à jour le mot de passe
                String updateSql = "UPDATE users SET mot_de_passe = ? WHERE id_user = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setString(1, newPass);
                updatePs.setInt(2, SessionManager.getInstance().getUserId());
                updatePs.executeUpdate();

                showAlert("Succès", "Mot de passe modifié avec succès 🔒");
            } else {
                showAlert("Erreur", "Mot de passe actuel incorrect");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la modification du mot de passe");
        }
    }

    // ===== VALIDATION =====
    private boolean validateFields() {
        if (prenomField.getText().trim().isEmpty() ||
                nomField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le prénom et le nom sont obligatoires");
            return false;
        }

        if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Erreur", "Email invalide");
            return false;
        }

        if (!phoneField.getText().matches("\\d{8,15}")) {
            showAlert("Erreur", "Numéro de téléphone invalide (8-15 chiffres)");
            return false;
        }

        return true;
    }

    // ===== NAVIGATION =====
    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("JobNest - Connexion");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("JobNest - Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== ALERT =====
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
