module tn.jobnest.gformation {
    // --- MODULES STANDARDS JAVA ET JAVAFX ---
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;

    // --- MODULES POUR L'IA ET LE JSON (OKHTTP & JACKSON) ---
    requires com.fasterxml.jackson.databind; // Pour transformer le JSON en objets
    requires com.fasterxml.jackson.core;     // Coeur de Jackson
    requires okhttp3;                        // Pour les requêtes API Gemini

    // Utilisation du nom de module correct pour Okio 3.x (JVM)
    // C'est cette ligne qui permet l'accès à okio.ByteString

    // -------------------------------------------------------
    requires javafx.media;
    // --- BIBLIOTHÈQUES TIERCES ---
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // --- CONFIGURATION DES ACCÈS (OUVERTURES ET EXPORTS) ---

    // 1. Ressources graphiques
    opens view to javafx.fxml;
    opens css to javafx.graphics;
    opens images to javafx.graphics, javafx.fxml;

    // 2. Modèles de données (Important pour Jackson et JavaFX TableView)
    opens tn.jobnest.gformation.model to javafx.base, com.fasterxml.jackson.databind;
    exports tn.jobnest.gformation.model;

    // 3. Contrôleurs (Injection FXML)
    opens tn.jobnest.gformation.Controller to javafx.fxml;
    exports tn.jobnest.gformation.Controller;

    // 4. Services et Logique métier
    exports tn.jobnest.gformation.services;
    opens tn.jobnest.gformation.services to com.fasterxml.jackson.databind;

    // 5. Repository et Accès DB
    exports tn.jobnest.gformation.repository;

    // 6. Application Principale
    exports tn.jobnest.gformation;
    opens tn.jobnest.gformation to javafx.fxml;
}