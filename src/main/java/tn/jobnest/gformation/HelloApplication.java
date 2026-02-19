package tn.jobnest.gformation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.jobnest.gformation.repository.DataSource;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Test de connexion immédiat dans la console
        DataSource.getInstance();

        // Chargement du fichier FXML depuis le dossier /view/
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/view/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("Gestion Formation - JobNest");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}