package com.yourapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.utils.StageUtils;
import com.utils.VerificationServer;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ── Démarrer le serveur de vérification email ──
        VerificationServer.start();

        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(root);

        stage.setTitle("JobNest - Recrutement moderne");
        stage.setScene(scene);
        StageUtils.forceMaximized(stage);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // ── Arrêter proprement le serveur à la fermeture de l'app ──
        VerificationServer.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}