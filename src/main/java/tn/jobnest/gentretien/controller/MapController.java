package tn.jobnest.gentretien.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Contrôleur de la fenêtre carte MapTiler.
 *
 * Flux :
 *  1. JavaFX charge map-view.fxml  →  initialize() est appelé
 *  2. Entretiencontroller appelle   setAdresse(lieu, titre)
 *  3. Quand la page HTML est prête  →  on exécute afficherAdresse(adresse) en JS
 */
public class MapController {

    @FXML private WebView webView;
    @FXML private Label   labelAdresse;
    @FXML private Label   labelTitre;
    @FXML private Button  btnFermer;

    private WebEngine webEngine;

    /** Adresse à envoyer à la carte (peut arriver avant que la page soit chargée) */
    private String adresseCible;
    private String titreCible;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Charger map.html depuis les ressources
        URL url = getClass().getResource("/tn/jobnest/gentretien/map.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("❌  map.html introuvable dans les ressources !");
        }

        // Dès que la page HTML est entièrement chargée, envoyer l'adresse
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED && adresseCible != null) {
                Platform.runLater(this::envoyerAdresse);
            }
        });
    }

    // ────────────────────────────────────────────────────────────────
    /**
     * À appeler depuis Entretiencontroller juste après loader.getController().
     *
     * @param adresse  adresse du lieu (ex : "123 Avenue Bourguiba, Tunis")
     * @param titre    titre du poste pour l'en-tête de la fenêtre
     */
    public void setAdresse(String adresse, String titre) {
        this.adresseCible = (adresse  != null) ? adresse.trim()  : "";
        this.titreCible   = (titre    != null) ? titre.trim()    : "Entretien";

        // Mettre à jour les labels de l'en-tête Java
        if (labelAdresse != null) labelAdresse.setText("📍 " + adresseCible);
        if (labelTitre   != null) labelTitre  .setText(titreCible);

        // Si la page est déjà chargée, envoyer l'adresse immédiatement
        if (webEngine != null &&
                webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            Platform.runLater(this::envoyerAdresse);
        }
        // Sinon le listener dans initialize() le fera
    }

    // ────────────────────────────────────────────────────────────────
    /**
     * Exécute la fonction JavaScript afficherAdresse('...') dans map.html.
     * Cette fonction géocode l'adresse et centre la carte + place un marqueur.
     */
    private void envoyerAdresse() {
        if (adresseCible == null || adresseCible.isEmpty()) return;
        try {
            // Échapper les caractères spéciaux pour une chaîne JS sûre
            String escaped = adresseCible
                    .replace("\\", "\\\\")
                    .replace("'",  "\\'")
                    .replace("\"", "\\\"")
                    .replace("\r", "")
                    .replace("\n", " ");

            webEngine.executeScript("afficherAdresse('" + escaped + "')");
        } catch (Exception ex) {
            System.err.println("Erreur executeScript : " + ex.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}