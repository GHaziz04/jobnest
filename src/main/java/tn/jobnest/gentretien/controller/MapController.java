package tn.jobnest.gentretien.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  MapController — SOLUTION DÉFINITIVE AU PROBLÈME DE CARTE COUPÉE
 *
 *  MÉTHODE : webEngine.loadContent(htmlString)
 *  ─────────────────────────────────────────────────────────────────
 *  Au lieu de webEngine.load(url), on lit map.html comme texte Java
 *  puis on l'injecte directement dans le WebView avec loadContent().
 *
 *  Pourquoi ça fonctionne ?
 *  - load(url) → JavaFX WebView charge depuis jar:// ou file://
 *    → taille du WebView pas encore calculée → Leaflet voit 0px
 *  - loadContent(string) → le contenu HTML est déjà en mémoire
 *    → WebView a sa taille finale avant de parser le HTML
 *    → Leaflet initialise correctement
 * ═══════════════════════════════════════════════════════════════════
 */
public class MapController {

    @FXML private WebView webView;
    @FXML private Label   labelAdresse;
    @FXML private Label   labelTitre;
    @FXML private Button  btnFermer;

    private WebEngine webEngine;
    private String adresseCible;
    private String titreCible;
    private boolean pageReady = false;

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Force le WebView à occuper tout l'espace disponible
        webView.setMinSize(0, 0);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ══════════════════════════════════════════════════════════
        //  LECTURE DU FICHIER HTML EN MÉMOIRE
        // ══════════════════════════════════════════════════════════
        String htmlContent = lireHtmlDepuisRessources();

        if (htmlContent == null) {
            System.err.println("❌ Impossible de lire map.html !");
            return;
        }

        // ══════════════════════════════════════════════════════════
        //  INJECTION DIRECTE DU HTML — LA CLÉ DE LA SOLUTION
        //  loadContent() au lieu de load()
        // ══════════════════════════════════════════════════════════
        webEngine.loadContent(htmlContent, "text/html");

        // ── Quand la page est prête ──────────────────────────────
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                pageReady = true;
                Platform.runLater(() -> {
                    // Envoyer l'adresse à la carte
                    if (adresseCible != null) envoyerAdresse();
                    // Forcer le recalcul de taille (sécurité)
                    forceInvalidateSize();
                    // Tenter d'obtenir la position GPS
                    tenterInjecterPositionGPS();
                });
            }
        });

        // ── Listener sur changements de taille ──────────────────
        webView.widthProperty().addListener((o, ov, nv)  -> { if (pageReady) forceInvalidateSize(); });
        webView.heightProperty().addListener((o, ov, nv) -> { if (pageReady) forceInvalidateSize(); });
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Lit le contenu de map.html depuis les ressources du projet.
     * Retourne le contenu HTML complet sous forme de String.
     */
    private String lireHtmlDepuisRessources() {
        try (InputStream is = getClass().getResourceAsStream("/tn/jobnest/gentretien/map.html")) {
            if (is == null) {
                System.err.println("❌ map.html introuvable dans /tn/jobnest/gentretien/");
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            String html = buffer.toString(StandardCharsets.UTF_8.name());
            System.out.println("✅ map.html lu : " + html.length() + " caractères");
            return html;
        } catch (Exception e) {
            System.err.println("❌ Erreur lecture map.html : " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Force Leaflet à recalculer sa taille en cascade.
     */
    private void forceInvalidateSize() {
        String js = "try{if(typeof map!=='undefined'&&map){map.invalidateSize({animate:false,pan:false});}}catch(e){}";
        try { webEngine.executeScript(js); } catch (Exception ignored) {}
        int[] delays = {100, 300, 600, 1200, 2500};
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
    /**
     * Appelé depuis Entretiencontroller après loader.getController()
     */
    public void setAdresse(String adresse, String titre) {
        this.adresseCible = (adresse != null) ? adresse.trim() : "";
        this.titreCible   = (titre   != null) ? titre.trim()   : "Entretien";

        if (labelAdresse != null) labelAdresse.setText("📍 " + adresseCible);
        if (labelTitre   != null) labelTitre  .setText(titreCible);

        if (pageReady) Platform.runLater(this::envoyerAdresse);
    }

    // ─────────────────────────────────────────────────────────────────
    private void envoyerAdresse() {
        if (adresseCible == null || adresseCible.isEmpty()) return;
        try {
            String escaped = adresseCible
                    .replace("\\", "\\\\").replace("'",  "\\'")
                    .replace("\"", "\\\"").replace("\r", "").replace("\n", " ");
            webEngine.executeScript("afficherAdresse('" + escaped + "')");
        } catch (Exception ex) {
            System.err.println("Erreur envoyerAdresse : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Géolocalisation par IP (ip-api.com — gratuit, sans clé).
     * Injecte setUserPosition(lat, lng) dans le HTML.
     */
    private void tenterInjecterPositionGPS() {
        Thread t = new Thread(() -> {
            try {
                java.net.URL api = new java.net.URL("http://ip-api.com/json/?fields=lat,lon,status");
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) api.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);

                if (c.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    String json = sb.toString();

                    if (json.contains("\"status\":\"success\"")) {
                        double lat = extraireDouble(json, "lat");
                        double lon = extraireDouble(json, "lon");
                        if (lat != 0.0 && lon != 0.0) {
                            final double fLat=lat, fLon=lon;
                            Platform.runLater(() -> {
                                try {
                                    webEngine.executeScript("setUserPosition("+fLat+","+fLon+")");
                                    System.out.println("✅ Position injectée : "+fLat+", "+fLon);
                                } catch (Exception ex) {
                                    System.err.println("Erreur GPS inject : "+ex.getMessage());
                                }
                            });
                        }
                    }
                }
                c.disconnect();
            } catch (Exception e) {
                System.out.println("ℹ️ GPS IP non disponible : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────
    private double extraireDouble(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx == -1) return 0.0;
            int start = idx + search.length(), end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) { return 0.0; }
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}