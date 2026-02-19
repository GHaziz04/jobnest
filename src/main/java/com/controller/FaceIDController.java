package com.controller;

import com.utils.DBConnection;
import com.utils.FaceIDAuthenticator;
import com.utils.SessionManager;
import com.utils.StageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Contrôleur pour la fenêtre d'authentification Face ID
 */
public class FaceIDController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button cancelButton;
    @FXML private Button retryButton;

    private FaceIDAuthenticator faceAuth;
    private Stage dialogStage;
    private Stage ownerStage;  // 🆕 Référence au stage parent
    private boolean authenticationSuccess = false;
    private Thread captureThread;
    private volatile boolean isRunning = false;
    private int authenticatedUserId = -1;  // 🆕 Stocker l'ID utilisateur

    @FXML
    public void initialize() {
        faceAuth = new FaceIDAuthenticator();
        retryButton.setVisible(false);
        startAuthentication();
    }

    /**
     * Démarrer le processus d'authentification
     */
    private void startAuthentication() {
        statusLabel.setText("🔍 Positionnez votre visage face à la caméra...");
        progressIndicator.setVisible(true);
        retryButton.setVisible(false);
        isRunning = true;

        captureThread = new Thread(() -> {
            try {
                // Appeler la méthode d'authentification avec callback pour afficher la caméra
                Integer userId = faceAuth.authenticateWithFace(this::updateCameraView);

                Platform.runLater(() -> {
                    isRunning = false;
                    if (userId != null) {
                        // Authentification réussie
                        authenticatedUserId = userId;
                        handleSuccessfulAuth(userId);
                    } else {
                        // Échec de l'authentification
                        handleFailedAuth();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    isRunning = false;
                    statusLabel.setText("❌ Erreur lors de l'authentification");
                    progressIndicator.setVisible(false);
                    retryButton.setVisible(true);
                    e.printStackTrace();
                });
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Mettre à jour l'affichage de la caméra
     */
    private void updateCameraView(Mat frame) {
        if (frame != null && !frame.empty()) {
            Image image = matToImage(frame);
            Platform.runLater(() -> {
                if (cameraView != null && isRunning) {
                    cameraView.setImage(image);
                }
            });
        }
    }

    /**
     * Convertir Mat OpenCV en Image JavaFX
     */
    private Image matToImage(Mat frame) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            return new Image(new ByteArrayInputStream(buffer.toArray()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gérer une authentification réussie
     */
    private void handleSuccessfulAuth(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE id_user = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Créer la session
                SessionManager.startSession(
                        rs.getInt("id_user"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("role")
                );

                System.out.println("✅ Session créée pour l'utilisateur: " + rs.getString("prenom") + " " + rs.getString("nom"));
                System.out.println("📧 Email: " + rs.getString("email"));
                System.out.println("👤 Rôle: " + rs.getString("role"));

                statusLabel.setText("✅ Authentification réussie !");
                progressIndicator.setVisible(false);
                authenticationSuccess = true;

                // Nettoyer les ressources
                System.out.println("🧹 Nettoyage des ressources temporaires...");

                // Attendre un peu pour montrer le message de succès
                Thread.sleep(1000);

                // Naviguer vers Home dans le thread JavaFX
                Platform.runLater(this::navigateToHome);

            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur lors de la récupération des données");
                    progressIndicator.setVisible(false);
                    retryButton.setVisible(true);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("❌ Erreur lors de la récupération des données");
                progressIndicator.setVisible(false);
                retryButton.setVisible(true);
            });
        }
    }

    /**
     * Gérer un échec d'authentification
     */
    private void handleFailedAuth() {
        statusLabel.setText("❌ Visage non reconnu. Réessayez ou utilisez l'email.");
        progressIndicator.setVisible(false);
        retryButton.setVisible(true);
    }

    /**
     * Réessayer l'authentification
     */
    @FXML
    private void retry() {
        startAuthentication();
    }

    /**
     * Annuler et fermer la fenêtre
     */
    @FXML
    private void cancel() {
        isRunning = false;
        if (captureThread != null && captureThread.isAlive()) {
            captureThread.interrupt();
        }
        if (faceAuth != null) {
            faceAuth.stopCamera();
        }
        closeDialog();
    }

    /**
     * Naviguer vers la page d'accueil
     */
    private void navigateToHome() {
        try {
            System.out.println("🏠 Navigation vers Home...");

            // Charger la page Home
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Home.fxml")
            );
            Parent root = loader.load();

            System.out.println("✅ Page Home chargée");

            // Utiliser le stage parent (celui qui a ouvert la dialog)
            Stage mainStage = ownerStage;

            if (mainStage != null) {
                System.out.println("🔄 Changement de scène...");

                // Changer la scène du stage principal
                mainStage.setScene(new Scene(root));
                mainStage.setTitle("Dashboard - JobNest");

                // Maximiser la fenêtre
                try {
                    StageUtils.forceMaximized(mainStage);
                } catch (Exception e) {
                    mainStage.setMaximized(true);
                }

                mainStage.show();

                System.out.println("✅ Navigation réussie");

                // Fermer la dialog Face ID après avoir changé la scène principale
                if (dialogStage != null) {
                    dialogStage.close();
                }
            } else {
                System.err.println("❌ Stage principal introuvable");
                showAlert("Erreur", "Impossible de naviguer vers la page d'accueil");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la navigation:");
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de la page d'accueil: " + e.getMessage());
        }
    }

    /**
     * Fermer la fenêtre de dialogue
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Définir le stage pour pouvoir le fermer
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;

        // Gérer la fermeture de la fenêtre
        stage.setOnCloseRequest(event -> {
            isRunning = false;
            if (captureThread != null && captureThread.isAlive()) {
                captureThread.interrupt();
            }
            if (faceAuth != null) {
                faceAuth.stopCamera();
            }
        });
    }

    /**
     * Définir le stage parent (owner)
     */
    public void setOwnerStage(Stage ownerStage) {
        this.ownerStage = ownerStage;
        System.out.println("✅ Stage parent défini");
    }

    /**
     * Vérifier si l'authentification a réussi
     */
    public boolean isAuthenticationSuccess() {
        return authenticationSuccess;
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Méthode statique pour ouvrir la fenêtre Face ID
     */
    public static boolean showFaceIDDialog(Stage ownerStage) {
        try {
            System.out.println("🔐 Ouverture de la fenêtre Face ID...");

            FXMLLoader loader = new FXMLLoader(
                    FaceIDController.class.getResource("/fxml/FaceIDAuth.fxml")
            );
            Parent root = loader.load();

            FaceIDController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Authentification Face ID");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ownerStage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            controller.setOwnerStage(ownerStage);  // 🆕 Passer le stage parent

            System.out.println("✅ Fenêtre Face ID configurée");

            dialogStage.showAndWait();

            boolean success = controller.isAuthenticationSuccess();
            System.out.println("🔐 Résultat authentification: " + (success ? "Succès" : "Échec"));

            return success;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture de Face ID:");
            e.printStackTrace();
            return false;
        }
    }
}