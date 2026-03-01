package tn.jobnest.gentretien.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  LocationPickerController
 *
 *  Fenêtre modale permettant au recruteur de choisir le lieu
 *  d'un entretien présentiel directement sur la carte Leaflet.
 *
 *  MÉTHODE DE COMMUNICATION HTML → JAVA (double sécurité) :
 *    1. Écoute du titre  (document.title = "CONFIRMED:adresse")
 *    2. Polling toutes les 300ms sur isConfirmed() via executeScript
 *
 *  FLUX :
 *  1. EntretienFormController ouvre cette fenêtre (showAndWait)
 *  2. Recruteur clique sur carte OU cherche une adresse
 *  3. Recruteur clique "Valider ce lieu"
 *  4. Java détecte la confirmation et ferme la fenêtre
 *  5. EntretienFormController récupère l'adresse via getAdresseChoisie()
 * ═══════════════════════════════════════════════════════════════════
 */
public class LocationPickerController {

    @FXML private WebView webView;
    @FXML private Label   labelInstruction;
    @FXML private Button  btnAnnuler;

    private WebEngine               webEngine;
    private boolean                 pageReady      = false;
    private String                  adresseChoisie = null;
    private ScheduledExecutorService scheduler;

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webView.setMinSize(0, 0);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ── Charger le HTML depuis les ressources ──
        String html = lireHtml("/tn/jobnest/gentretien/location-picker.html");
        if (html == null) {
            System.err.println("location-picker.html introuvable !");
            return;
        }
        webEngine.loadContent(html, "text/html");

        // ── Méthode 1 : écoute du titre ──
        webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("CONFIRMED:")) {
                String addrFromTitle = newTitle.substring("CONFIRMED:".length()).trim();
                Platform.runLater(() -> {
                    try {
                        Object jsAddr = webEngine.executeScript("getSelectedAddress()");
                        adresseChoisie = (jsAddr != null && !jsAddr.toString().isEmpty())
                                ? jsAddr.toString() : addrFromTitle;
                    } catch (Exception e) {
                        adresseChoisie = addrFromTitle;
                    }
                    stopPolling();
                    fermerFenetre();
                });
            }
        });

        // ── Quand la page est prête ──
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                pageReady = true;
                Platform.runLater(() -> {
                    forceInvalidate();
                    // Méthode 2 : polling (filet de sécurité)
                    startPolling();
                });
            }
        });

        webView.widthProperty() .addListener((o, ov, nv) -> { if (pageReady) forceInvalidate(); });
        webView.heightProperty().addListener((o, ov, nv) -> { if (pageReady) forceInvalidate(); });
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Polling toutes les 300ms — vérifie si l'utilisateur a validé.
     * Filet de sécurité si le listener de titre échoue.
     */
    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "location-picker-poll");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() ->
                        Platform.runLater(() -> {
                            if (!pageReady) return;
                            try {
                                Object confirmed = webEngine.executeScript("isConfirmed()");
                                if (Boolean.TRUE.equals(confirmed)) {
                                    Object jsAddr = webEngine.executeScript("getSelectedAddress()");
                                    if (jsAddr != null && !jsAddr.toString().isEmpty()) {
                                        adresseChoisie = jsAddr.toString();
                                        stopPolling();
                                        fermerFenetre();
                                    }
                                }
                            } catch (Exception ignored) {}
                        }),
                500, 300, TimeUnit.MILLISECONDS);
    }

    // ─────────────────────────────────────────────────────────────────
    private void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private void forceInvalidate() {
        String js = "try{if(typeof map!=='undefined'&&map){"
                + "map.invalidateSize({animate:false,pan:false});}}catch(e){}";
        try { webEngine.executeScript(js); } catch (Exception ignored) {}
        int[] delays = {150, 400, 800, 1500, 3000};
        for (int d : delays) {
            Thread t = new Thread(() -> {
                try { Thread.sleep(d); } catch (InterruptedException ex) { return; }
                Platform.runLater(() -> {
                    try { webEngine.executeScript(js); } catch (Exception ignored) {}
                });
            });
            t.setDaemon(true);
            t.start();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void annuler() {
        adresseChoisie = null;
        stopPolling();
        fermerFenetre();
    }

    // ─────────────────────────────────────────────────────────────────
    private void fermerFenetre() {
        stopPolling();
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Appelé par EntretienFormController après showAndWait().
     * @return l'adresse choisie, ou null si annulé
     */
    public String getAdresseChoisie() {
        return adresseChoisie;
    }

    // ─────────────────────────────────────────────────────────────────
    private String lireHtml(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
            return buf.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            System.err.println("Erreur lecture " + resourcePath + " : " + e.getMessage());
            return null;
        }
    }
}