package com.controller;

import com.utils.SessionManager;
import com.utils.StageUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Button adminButton;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        welcomeLabel.setText("Bienvenue " + SessionManager.getInstance().getPrenom() + " 👋");

        // Afficher le bouton Admin seulement si l'utilisateur est Admin
        if (isAdmin()) {
            adminButton.setVisible(true);
            adminButton.setManaged(true);
        } else {
            adminButton.setVisible(false);
            adminButton.setManaged(false);
        }
    }

    private boolean isAdmin() {
        return SessionManager.getInstance().getRole().equalsIgnoreCase("Admin");
    }

    @FXML
    private void openAdminDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AdminDashboard.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openProfile() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Profile.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profil - JobNest");
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
}