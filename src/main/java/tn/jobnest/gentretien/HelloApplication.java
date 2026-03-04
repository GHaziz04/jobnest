package tn.jobnest.gentretien;

import tn.jobnest.gentretien.voice.VoiceServer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // ✅ DÉMARRER LE SERVEUR VOCAL ICI — avant d'afficher la fenêtre
        VoiceServer.start();

        Parent root = FXMLLoader.load(
                getClass().getResource("/tn/jobnest/gentretien/entretien-view.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());

        stage.setTitle("JobNest - Gestion des Entretiens");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.centerOnScreen();
        stage.setResizable(true);

        // ✅ ARRÊTER LE SERVEUR PROPREMENT quand on ferme l'appli
        stage.setOnCloseRequest(event -> VoiceServer.stop());

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}