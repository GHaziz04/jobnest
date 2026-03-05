package com.controller;

import com.utils.DBConnection;
import com.utils.TwoFactorAuth;
import com.utils.PreferencesManager;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.ResourceBundle;

public class ProfileController {

    private static final String STABILITY_API_KEY = "sk-XdeTEqH5eHvoUKX99K7rsUMgwQwIbyWzZfZJinKmX57Q3w1H";

    // ===== HEADER =====
    @FXML private Label welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;
    @FXML private Label joinDateLabel;

    // ===== FORM FIELDS =====
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField adresseField;

    // ===== BUTTONS =====
    @FXML private Button editButton;
    @FXML private HBox actionButtons;
    @FXML private Button generateAvatarBtn;
    @FXML private Button backBtn;
    @FXML private Button logoutBtn;
    @FXML private Button changePhotoBtn;
    @FXML private Button changePasswordBtn;
    @FXML private Button deleteAccountBtn;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    // ===== STATISTICS =====
    @FXML private Label stat1Value;
    @FXML private Label stat1Label;
    @FXML private Label stat2Value;
    @FXML private Label stat2Label;
    @FXML private Label stat3Value;
    @FXML private Label stat3Label;

    // ===== PREFERENCES =====
    @FXML private Button notifToggle;
    @FXML private Button emailToggle;
    @FXML private Button visibilityToggle;
    @FXML private Button twoFaToggle;
    @FXML private Button themeLightBtn;
    @FXML private Button themeDarkBtn;
    @FXML private ComboBox<String> langCombo;

    // ===== LABELS TRADUISIBLES =====
    @FXML private Label sectionPersonalTitle;
    @FXML private Label sectionPersonalSubtitle;
    @FXML private Label sectionSecurityTitle;
    @FXML private Label sectionSecuritySubtitle;
    @FXML private Label sectionPrefsTitle;
    @FXML private Label sectionPrefsSubtitle;
    @FXML private Label sectionDangerTitle;
    @FXML private Label sectionDangerSubtitle;
    @FXML private Label labelPrenom;
    @FXML private Label labelNom;
    @FXML private Label labelEmail;
    @FXML private Label labelPhone;
    @FXML private Label labelAdresse;
    @FXML private Label labelTheme;
    @FXML private Label labelThemeDesc;
    @FXML private Label labelLangue;
    @FXML private Label labelLangueDesc;
    @FXML private Label labelNotif;
    @FXML private Label labelNotifDesc;
    @FXML private Label labelEmails;
    @FXML private Label labelEmailsDesc;
    @FXML private Label labelVisibility;
    @FXML private Label labelVisibilityDesc;
    @FXML private Label labelTwoFa;
    @FXML private Label labelTwoFaDesc;
    @FXML private Label labelPasswordTitle;
    @FXML private Label labelPasswordDesc;

    // ===== STATE =====
    private String imagePath;
    private boolean editMode = false;
    private ResourceBundle bundle;

    // =========================================================
    // ===== INIT =====
    // =========================================================
    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) { redirectToLogin(); return; }

        welcomeLabel.setText(SessionManager.getInstance().getPrenom() + " 👋");

        // ComboBox langue AVANT loadFromDB
        langCombo.getItems().addAll("🇫🇷 Français", "🇬🇧 English", "🇦🇪 العربية");
        langCombo.setValue("🇫🇷 Français");

        // Charger les prefs depuis BDD
        PreferencesManager prefs = PreferencesManager.getInstance();
        prefs.loadFromDB(SessionManager.getInstance().getId());

        // Appliquer langue (initialise bundle + updateLabels)
        applyLanguage(prefs.getLangue());

        loadUserProfile();
        loadStatistics();
        applyCircularClip();

        // Thème et sync après que la scene soit prête
        javafx.application.Platform.runLater(() -> {
            applyTheme(prefs.getTheme());
            syncToggleStates();
        });
    }

    // =========================================================
    // ===== CIRCULAR CLIP =====
    // =========================================================
    private void applyCircularClip() {
        double radius = profileImageView.getFitWidth() / 2;
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(radius, radius, radius);
        profileImageView.setClip(clip);
        profileImageView.setPreserveRatio(false);
    }

    // =========================================================
    // ===== THEME =====
    // =========================================================
    private void applyTheme(String theme) {
        javafx.scene.Scene scene = welcomeLabel.getScene();
        if (scene == null) return;
        scene.getStylesheets().removeIf(s -> s.contains("profile-dark"));
        if ("dark".equals(theme)) {
            try {
                scene.getStylesheets().add(getClass().getResource("/css/profile-dark.css").toExternalForm());
            } catch (Exception e) {
                System.err.println("⚠️ profile-dark.css introuvable");
            }
        }
    }

    @FXML private void setThemeLight() {
        PreferencesManager.getInstance().saveTheme(SessionManager.getInstance().getId(), "light");
        themeLightBtn.getStyleClass().setAll("theme-btn-active");
        themeDarkBtn.getStyleClass().setAll("theme-btn-inactive");
        applyTheme("light");
    }

    @FXML private void setThemeDark() {
        PreferencesManager.getInstance().saveTheme(SessionManager.getInstance().getId(), "dark");
        themeDarkBtn.getStyleClass().setAll("theme-btn-active");
        themeLightBtn.getStyleClass().setAll("theme-btn-inactive");
        applyTheme("dark");
    }

    // =========================================================
    // ===== LANGUE =====
    // =========================================================
    private void applyLanguage(String lang) {
        Locale locale = switch (lang) {
            case "en" -> Locale.ENGLISH;
            case "ar" -> new Locale("ar");
            default  -> Locale.FRENCH;
        };
        try {
            bundle = ResourceBundle.getBundle("lang.messages", locale);
        } catch (Exception e) {
            try { bundle = ResourceBundle.getBundle("lang.messages", Locale.FRENCH); }
            catch (Exception ex) { System.err.println("⚠️ Fichiers de langue introuvables : " + ex.getMessage()); }
        }
        updateLabels();
    }

    private void updateLabels() {
        if (bundle == null) return;
        try {
            // Navigation
            backBtn.setText(bundle.getString("profile.back"));
            logoutBtn.setText(bundle.getString("profile.logout"));

            // Photo / Avatar
            changePhotoBtn.setText(bundle.getString("profile.changePhoto"));
            generateAvatarBtn.setText(bundle.getString("profile.generateAvatar"));

            // Section Infos
            sectionPersonalTitle.setText(bundle.getString("profile.personal"));
            sectionPersonalSubtitle.setText(bundle.getString("profile.personal.sub"));
            editButton.setText(bundle.getString("profile.edit"));

            // Champs
            labelPrenom.setText(bundle.getString("profile.firstname"));
            labelNom.setText(bundle.getString("profile.lastname"));
            labelEmail.setText(bundle.getString("profile.email"));
            labelPhone.setText(bundle.getString("profile.phone"));
            labelAdresse.setText(bundle.getString("profile.address"));

            // Boutons action
            saveBtn.setText(bundle.getString("profile.save"));
            cancelBtn.setText(bundle.getString("profile.cancel"));

            // Section Sécurité
            sectionSecurityTitle.setText(bundle.getString("profile.security"));
            sectionSecuritySubtitle.setText(bundle.getString("profile.security.sub"));
            labelPasswordTitle.setText(bundle.getString("profile.password.title"));
            labelPasswordDesc.setText(bundle.getString("profile.password.desc"));
            changePasswordBtn.setText(bundle.getString("profile.changePassword"));
            labelTwoFa.setText(bundle.getString("profile.twofa"));
            labelTwoFaDesc.setText(bundle.getString("profile.twofa.desc"));

            // Section Préférences
            sectionPrefsTitle.setText(bundle.getString("profile.preferences"));
            sectionPrefsSubtitle.setText(bundle.getString("profile.preferences.sub"));
            labelTheme.setText(bundle.getString("profile.theme"));
            labelThemeDesc.setText(bundle.getString("profile.theme.desc"));
            themeLightBtn.setText(bundle.getString("profile.themeLight"));
            themeDarkBtn.setText(bundle.getString("profile.themeDark"));
            labelLangue.setText(bundle.getString("profile.language"));
            labelLangueDesc.setText(bundle.getString("profile.language.desc"));
            labelNotif.setText(bundle.getString("profile.notifications"));
            labelNotifDesc.setText(bundle.getString("profile.notifications.desc"));
            labelEmails.setText(bundle.getString("profile.emails"));
            labelEmailsDesc.setText(bundle.getString("profile.emails.desc"));
            labelVisibility.setText(bundle.getString("profile.visibility"));
            labelVisibilityDesc.setText(bundle.getString("profile.visibility.desc"));

            // Section Danger
            sectionDangerTitle.setText(bundle.getString("profile.danger"));
            sectionDangerSubtitle.setText(bundle.getString("profile.danger.sub"));
            deleteAccountBtn.setText(bundle.getString("profile.deleteAccount"));

            // Toggles (texte ACTIVÉ/DÉSACTIVÉ traduit)
            PreferencesManager prefs = PreferencesManager.getInstance();
            setToggle(notifToggle,      prefs.isNotifEnabled());
            setToggle(emailToggle,      prefs.isEmailEnabled());
            setToggle(visibilityToggle, prefs.isProfilVisible());
            setToggle(twoFaToggle,      prefs.isTwoFaEnabled());

        } catch (Exception e) {
            System.err.println("Clé manquante dans bundle : " + e.getMessage());
        }
    }

    @FXML
    private void changeLanguage() {
        if (langCombo.getValue() == null) return;
        String lang = switch (langCombo.getValue()) {
            case "🇬🇧 English" -> "en";
            case "🇦🇪 العربية" -> "ar";
            default -> "fr";
        };
        PreferencesManager.getInstance().saveLangue(SessionManager.getInstance().getId(), lang);
        applyLanguage(lang);
    }

    // =========================================================
    // ===== SYNC TOGGLES =====
    // =========================================================
    private void syncToggleStates() {
        PreferencesManager prefs = PreferencesManager.getInstance();
        if ("dark".equals(prefs.getTheme())) {
            themeDarkBtn.getStyleClass().setAll("theme-btn-active");
            themeLightBtn.getStyleClass().setAll("theme-btn-inactive");
        } else {
            themeLightBtn.getStyleClass().setAll("theme-btn-active");
            themeDarkBtn.getStyleClass().setAll("theme-btn-inactive");
        }
        switch (prefs.getLangue()) {
            case "en" -> langCombo.setValue("🇬🇧 English");
            case "ar" -> langCombo.setValue("🇦🇪 العربية");
            default  -> langCombo.setValue("🇫🇷 Français");
        }
        setToggle(notifToggle,      prefs.isNotifEnabled());
        setToggle(emailToggle,      prefs.isEmailEnabled());
        setToggle(visibilityToggle, prefs.isProfilVisible());
        setToggle(twoFaToggle,      prefs.isTwoFaEnabled());
    }

    private void setToggle(Button btn, boolean enabled) {
        if (btn == null) return;
        try {
            btn.setText(bundle != null ? bundle.getString(enabled ? "profile.enabled" : "profile.disabled")
                    : (enabled ? "ACTIVÉ" : "DÉSACTIVÉ"));
        } catch (Exception e) {
            btn.setText(enabled ? "ACTIVÉ" : "DÉSACTIVÉ");
        }
        btn.getStyleClass().setAll(enabled ? "toggle-on" : "toggle-off");
    }

    // =========================================================
    // ===== TOGGLES PRÉFÉRENCES =====
    // =========================================================
    @FXML private void toggleNotifications() {
        boolean v = !PreferencesManager.getInstance().isNotifEnabled();
        PreferencesManager.getInstance().saveNotif(SessionManager.getInstance().getId(), v);
        setToggle(notifToggle, v);
    }
    @FXML private void toggleEmails() {
        boolean v = !PreferencesManager.getInstance().isEmailEnabled();
        PreferencesManager.getInstance().saveEmail(SessionManager.getInstance().getId(), v);
        setToggle(emailToggle, v);
    }
    @FXML private void toggleVisibility() {
        boolean v = !PreferencesManager.getInstance().isProfilVisible();
        PreferencesManager.getInstance().saveProfilVisible(SessionManager.getInstance().getId(), v);
        setToggle(visibilityToggle, v);
    }
    @FXML private void toggleTwoFA() {
        boolean currentlyEnabled = PreferencesManager.getInstance().isTwoFaEnabled();

        if (!currentlyEnabled) {
            // ── Activer 2FA → ouvrir le dialog de setup ──────────────
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/TwoFASetupDialog.fxml"));
                Parent root = loader.load();
                root.getStylesheets().add(
                        getClass().getResource("/css/twofa-dialog.css").toExternalForm());

                TwoFASetupController controller = loader.getController();

                Stage stage = new Stage();
                stage.setTitle("🔐 Configurer le 2FA - JobNest");
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.initOwner(welcomeLabel.getScene().getWindow());
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.showAndWait();

                if (controller.isActivated()) {
                    PreferencesManager.getInstance().saveTwoFA(
                            SessionManager.getInstance().getId(), true);
                    setToggle(twoFaToggle, true);
                    showAlert("Sécurité ✅", "🎉 Double authentification activée !\n" +
                            "À chaque connexion, votre code Google Authenticator sera demandé.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible d'ouvrir la configuration 2FA");
            }

        } else {
            // ── Désactiver 2FA → confirmation ────────────────────────
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Désactiver le 2FA");
            confirm.setHeaderText("⚠️ Désactiver la double authentification ?");
            confirm.setContentText(
                    "Votre compte sera moins sécurisé.\n" +
                            "Êtes-vous sûr de vouloir désactiver le 2FA ?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    TwoFactorAuth.disableTwoFA(SessionManager.getInstance().getId());
                    PreferencesManager.getInstance().saveTwoFA(
                            SessionManager.getInstance().getId(), false);
                    setToggle(twoFaToggle, false);
                    showAlert("Info", "2FA désactivé.");
                }
            });
        }
    }

    // =========================================================
    // ===== LOAD USER PROFILE =====
    // =========================================================
    private void loadUserProfile() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id_user = ?");
            ps.setInt(1, SessionManager.getInstance().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                fullNameLabel.setText(rs.getString("prenom") + " " + rs.getString("nom"));
                roleLabel.setText(rs.getString("role"));
                emailLabel.setText(rs.getString("email"));
                prenomField.setText(rs.getString("prenom"));
                nomField.setText(rs.getString("nom"));
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("telephone"));
                adresseField.setText(rs.getString("adresse"));
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

    // =========================================================
    // ===== LOAD STATISTICS =====
    // =========================================================
    private void loadStatistics() {
        String role = SessionManager.getInstance().getRole();
        try (Connection conn = DBConnection.getConnection()) {
            if (role.equalsIgnoreCase("Candidat"))       loadCandidatStats(conn);
            else if (role.equalsIgnoreCase("Recruteur")) loadRecruteurStats(conn);
            else if (role.equalsIgnoreCase("Formateur")) loadFormateurStats(conn);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadCandidatStats(Connection conn) throws Exception {
        int u = SessionManager.getInstance().getId();
        PreparedStatement p1 = conn.prepareStatement("SELECT COUNT(*) FROM candidature c JOIN candidat ca ON c.id_candidat=ca.id_candidat WHERE ca.id_user=?");
        p1.setInt(1,u); ResultSet r1=p1.executeQuery();
        if(r1.next()){stat1Value.setText(String.valueOf(r1.getInt(1)));stat1Label.setText("Candidatures");}
        PreparedStatement p2 = conn.prepareStatement("SELECT COUNT(*) FROM entretien e JOIN candidature c ON e.id_candidature=c.id_candidature JOIN candidat ca ON c.id_candidat=ca.id_candidat WHERE ca.id_user=?");
        p2.setInt(1,u); ResultSet r2=p2.executeQuery();
        if(r2.next()){stat2Value.setText(String.valueOf(r2.getInt(1)));stat2Label.setText("Entretiens");}
        stat3Value.setText("0"); stat3Label.setText("Formations");
    }

    private void loadRecruteurStats(Connection conn) throws Exception {
        int u = SessionManager.getInstance().getId();
        PreparedStatement p1=conn.prepareStatement("SELECT COUNT(*) FROM offre o JOIN recruteur r ON o.id_recruteur=r.id_recruteur WHERE r.id_user=?");
        p1.setInt(1,u); ResultSet r1=p1.executeQuery();
        if(r1.next()){stat1Value.setText(String.valueOf(r1.getInt(1)));stat1Label.setText("Offres");}
        PreparedStatement p2=conn.prepareStatement("SELECT COUNT(*) FROM candidature c JOIN offre o ON c.id_offre=o.id_offre JOIN recruteur r ON o.id_recruteur=r.id_recruteur WHERE r.id_user=?");
        p2.setInt(1,u); ResultSet r2=p2.executeQuery();
        if(r2.next()){stat2Value.setText(String.valueOf(r2.getInt(1)));stat2Label.setText("Candidatures");}
        PreparedStatement p3=conn.prepareStatement("SELECT COUNT(*) FROM entretien e JOIN candidature c ON e.id_candidature=c.id_candidature JOIN offre o ON c.id_offre=o.id_offre JOIN recruteur r ON o.id_recruteur=r.id_recruteur WHERE r.id_user=?");
        p3.setInt(1,u); ResultSet r3=p3.executeQuery();
        if(r3.next()){stat3Value.setText(String.valueOf(r3.getInt(1)));stat3Label.setText("Entretiens");}
    }

    private void loadFormateurStats(Connection conn) {
        stat1Value.setText("0");stat1Label.setText("Formations");
        stat2Value.setText("0");stat2Label.setText("Participants");
        stat3Value.setText("0");stat3Label.setText("Cours");
    }

    // =========================================================
    // ===== EDIT MODE =====
    // =========================================================
    @FXML private void toggleEditMode() {
        editMode = !editMode;
        prenomField.setEditable(editMode); nomField.setEditable(editMode);
        emailField.setEditable(editMode);  phoneField.setEditable(editMode);
        adresseField.setEditable(editMode);
        actionButtons.setVisible(editMode); actionButtons.setManaged(editMode);
        try { editButton.setText(editMode ? "👁 " + bundle.getString("profile.read") : bundle.getString("profile.edit")); }
        catch (Exception e) { editButton.setText(editMode ? "👁 Lecture" : "✏️ Modifier"); }
    }

    @FXML private void saveProfile() {
        if (!validateFields()) return;
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET prenom=?,nom=?,email=?,telephone=?,adresse=?,photo_profil=? WHERE id_user=?");
            ps.setString(1,prenomField.getText().trim()); ps.setString(2,nomField.getText().trim());
            ps.setString(3,emailField.getText().trim());  ps.setString(4,phoneField.getText().trim());
            ps.setString(5,adresseField.getText().trim()); ps.setString(6,imagePath);
            ps.setInt(7,SessionManager.getInstance().getId());
            if(ps.executeUpdate()>0){
                SessionManager.getInstance().setPrenom(prenomField.getText().trim());
                SessionManager.getInstance().setNom(nomField.getText().trim());
                SessionManager.getInstance().setEmail(emailField.getText().trim());
                showAlert("Succès","Profil mis à jour 🎉");
                toggleEditMode(); loadUserProfile();
            }
        } catch(Exception e){e.printStackTrace();showAlert("Erreur","Erreur mise à jour");}
    }

    @FXML private void cancelEdit(){toggleEditMode();loadUserProfile();}

    // =========================================================
    // ===== PHOTO =====
    // =========================================================
    @FXML private void changeProfileImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg"));
        File file = chooser.showOpenDialog(profileImageView.getScene().getWindow());
        if(file!=null){
            imagePath=file.getAbsolutePath();
            profileImageView.setImage(new Image(file.toURI().toString()));
            saveProfileImage();
        }
    }

    private void saveProfileImage() {
        try(Connection conn=DBConnection.getConnection()){
            PreparedStatement ps=conn.prepareStatement("UPDATE users SET photo_profil=? WHERE id_user=?");
            ps.setString(1,imagePath); ps.setInt(2,SessionManager.getInstance().getUserId());
            ps.executeUpdate(); showAlert("Succès","Photo mise à jour ✅");
        }catch(Exception e){e.printStackTrace();}
    }

    // =========================================================
    // ===== GENERATE AI AVATAR =====
    // =========================================================
    @FXML private void generateAIAvatar() {
        if(imagePath==null||imagePath.isEmpty()||imagePath.startsWith("http")){
            showAlert("Info","📸 Veuillez d'abord choisir une photo de profil"); return;}
        File imageFile=new File(imagePath);
        if(!imageFile.exists()){showAlert("Erreur","Photo introuvable.");return;}
        showAlert("Info","🎨 Génération en cours... (10-20 secondes)");
        new Thread(()->{
            try{
                java.awt.image.BufferedImage original=javax.imageio.ImageIO.read(imageFile);
                java.awt.image.BufferedImage resized=new java.awt.image.BufferedImage(1024,1024,java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d=resized.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(original,0,0,1024,1024,null); g2d.dispose();
                File tempFile=new File(System.getProperty("user.home")+"/JobNest/temp_resized.png");
                new File(System.getProperty("user.home")+"/JobNest/").mkdirs();
                javax.imageio.ImageIO.write(resized,"png",tempFile);
                byte[] imageBytes=java.nio.file.Files.readAllBytes(tempFile.toPath());
                String boundary="----Boundary"+System.currentTimeMillis();
                java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"init_image\"; filename=\"resized.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());
                baos.write(imageBytes); baos.write("\r\n".getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"init_image_mode\"\r\n\r\nIMAGE_STRENGTH\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image_strength\"\r\n\r\n0.25\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"text_prompts[0][text]\"\r\n\r\nhighly detailed cartoon portrait, pixar 3d animation style, smooth skin, big expressive eyes, vibrant colors, professional lighting, centered face, clean gradient background, sharp focus, disney character style, friendly smile, high quality render\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"text_prompts[0][weight]\"\r\n\r\n1\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"text_prompts[1][text]\"\r\n\r\nrealistic, photo, blurry, low quality, ugly, deformed, bad anatomy, watermark, text, grainy\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"text_prompts[1][weight]\"\r\n\r\n-1\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"cfg_scale\"\r\n\r\n10\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"samples\"\r\n\r\n1\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"steps\"\r\n\r\n50\r\n").getBytes());
                baos.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"style_preset\"\r\n\r\n3d-model\r\n").getBytes());
                baos.write(("--"+boundary+"--\r\n").getBytes());
                java.net.http.HttpClient client=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
                java.net.http.HttpRequest request=java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image"))
                        .header("Content-Type","multipart/form-data; boundary="+boundary)
                        .header("Authorization","Bearer "+STABILITY_API_KEY)
                        .header("Accept","application/json")
                        .timeout(java.time.Duration.ofSeconds(90))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray())).build();
                java.net.http.HttpResponse<String> response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()==200){
                    String body=response.body();
                    int b64Start=body.indexOf("\"base64\":\"")+10;
                    int b64End=body.indexOf("\"",b64Start);
                    byte[] resultBytes=java.util.Base64.getDecoder().decode(body.substring(b64Start,b64End));
                    String savePath=System.getProperty("user.home")+"/JobNest/avatars/";
                    new File(savePath).mkdirs();
                    File outputFile=new File(savePath+"avatar_ai_"+SessionManager.getInstance().getId()+".png");
                    try(java.io.FileOutputStream fos=new java.io.FileOutputStream(outputFile)){fos.write(resultBytes);}
                    imagePath=outputFile.getAbsolutePath();
                    try(Connection conn=DBConnection.getConnection()){
                        PreparedStatement ps=conn.prepareStatement("UPDATE users SET photo_profil=? WHERE id_user=?");
                        ps.setString(1,imagePath); ps.setInt(2,SessionManager.getInstance().getId()); ps.executeUpdate();
                    }
                    javafx.scene.image.Image avatarImage=new javafx.scene.image.Image(outputFile.toURI().toString());
                    javafx.application.Platform.runLater(()->{profileImageView.setImage(avatarImage);showAlert("Succès","🎉 Avatar cartoon généré !");});
                } else if(response.statusCode()==401){javafx.application.Platform.runLater(()->showAlert("Erreur","❌ Clé API invalide."));}
                else if(response.statusCode()==402){javafx.application.Platform.runLater(()->showAlert("Crédits","💳 Plus de crédits Stability AI."));}
                else{String err=response.body();javafx.application.Platform.runLater(()->showAlert("Erreur "+response.statusCode(),err));}
            }catch(java.net.http.HttpTimeoutException e){javafx.application.Platform.runLater(()->showAlert("Timeout","⏱️ Réessaye."));}
            catch(Exception e){e.printStackTrace();javafx.application.Platform.runLater(()->showAlert("Erreur",e.getMessage()));}
        }).start();
    }

    // =========================================================
    // ===== CHANGE PASSWORD =====
    // =========================================================
    @FXML private void changePassword() {
        try{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/ChangePasswordDialog.fxml"));
            Parent root=loader.load();
            root.getStylesheets().add(getClass().getResource("/css/password-dialog.css").toExternalForm());
            Stage s=new Stage(); s.setTitle("Changer le mot de passe");
            s.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            s.initOwner(welcomeLabel.getScene().getWindow());
            s.setScene(new Scene(root)); s.setResizable(false);
            ChangePasswordDialogController c=loader.getController();
            s.showAndWait();
            if(c.isValid()) updatePassword(c.getCurrentPassword(),c.getNewPassword(),c.getConfirmPassword());
        }catch(Exception e){e.printStackTrace();showAlert("Erreur","Impossible d'ouvrir le dialogue");}
    }

    private void updatePassword(String current,String newPass,String confirm){
        if(current.isEmpty()||newPass.isEmpty()||confirm.isEmpty()){showAlert("Erreur","Remplissez tous les champs");return;}
        if(!newPass.equals(confirm)){showAlert("Erreur","Les mots de passe ne correspondent pas");return;}
        if(newPass.length()<6){showAlert("Erreur","Minimum 6 caractères");return;}
        try(Connection conn=DBConnection.getConnection()){
            PreparedStatement cp=conn.prepareStatement("SELECT mot_de_passe FROM users WHERE id_user=?");
            cp.setInt(1,SessionManager.getInstance().getUserId());
            ResultSet rs=cp.executeQuery();
            if(rs.next()&&rs.getString("mot_de_passe").equals(current)){
                PreparedStatement up=conn.prepareStatement("UPDATE users SET mot_de_passe=? WHERE id_user=?");
                up.setString(1,newPass); up.setInt(2,SessionManager.getInstance().getUserId()); up.executeUpdate();
                showAlert("Succès","Mot de passe modifié 🔒");
            }else showAlert("Erreur","Mot de passe actuel incorrect");
        }catch(Exception e){e.printStackTrace();}
    }

    // =========================================================
    // ===== DELETE ACCOUNT =====
    // =========================================================
    @FXML private void deleteAccount(){
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le compte");
        confirm.setHeaderText("⚠️ Êtes-vous sûr ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r->{
            if(r==ButtonType.OK){
                try(Connection conn=DBConnection.getConnection()){
                    PreparedStatement ps=conn.prepareStatement("DELETE FROM users WHERE id_user=?");
                    ps.setInt(1,SessionManager.getInstance().getId()); ps.executeUpdate();
                    SessionManager.clearSession();
                    javafx.application.Platform.runLater(this::redirectToLogin);
                }catch(Exception e){e.printStackTrace();showAlert("Erreur","Impossible de supprimer");}
            }
        });
    }

    // =========================================================
    // ===== VALIDATION =====
    // =========================================================
    private boolean validateFields(){
        if(prenomField.getText().trim().isEmpty()||nomField.getText().trim().isEmpty()){showAlert("Erreur","Prénom et nom obligatoires");return false;}
        if(!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")){showAlert("Erreur","Email invalide");return false;}
        if(!phoneField.getText().matches("\\d{8,15}")){showAlert("Erreur","Numéro invalide (8-15 chiffres)");return false;}
        return true;
    }

    // =========================================================
    // ===== NAVIGATION =====
    // =========================================================
    @FXML private void goBack(){
        try{Parent root=FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            Stage stage=(Stage)welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);stage.show();
        }catch(Exception e){e.printStackTrace();}
    }

    @FXML private void logout(){
        try{SessionManager.clearSession();
            Parent root=FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage=(Stage)welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));stage.setTitle("JobNest - Connexion");
            StageUtils.forceMaximized(stage);stage.show();
        }catch(Exception e){e.printStackTrace();}
    }

    private void redirectToLogin(){
        try{Parent root=FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage=(Stage)welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));stage.setTitle("JobNest - Connexion");stage.show();
        }catch(Exception e){e.printStackTrace();}
    }

    private void showAlert(String title,String message){
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);alert.setHeaderText(null);alert.setContentText(message);alert.showAndWait();
    }
}
