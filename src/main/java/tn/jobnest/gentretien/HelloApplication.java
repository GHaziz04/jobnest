package tn.jobnest.gentretien;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/tn/jobnest/gentretien/entretien-view.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());

        // ===================================
        // 🔧 CONFIGURATION DE LA FENÊTRE
        // ===================================
        stage.setTitle("JobNest - Gestion des Entretiens");
        stage.setScene(scene);

        // Définir une taille minimale
        stage.setMinWidth(1200);
        stage.setMinHeight(700);

        // Taille par défaut
        stage.setWidth(1400);
        stage.setHeight(850);

        // Centrer la fenêtre sur l'écran
        stage.centerOnScreen();

        // Permettre le redimensionnement
        stage.setResizable(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}