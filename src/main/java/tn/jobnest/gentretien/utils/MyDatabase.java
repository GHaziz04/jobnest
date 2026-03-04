package tn.jobnest.gentretien.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * VERSION FINALE
 *
 * PROBLÈME CORRIGÉ :
 * Le thread KeepAlive exécutait "SELECT 1" EN MÊME TEMPS qu'une requête normale
 * → NullPointerException sur le ResultSet car la connexion était occupée.
 *
 * SOLUTION :
 * - getConn() est synchronized(this) → toute lecture/écriture est sérialisée
 * - Le KeepAlive utilise aussi synchronized(this) → il attend que la requête
 *   en cours soit terminée avant d'envoyer le ping
 * - Résultat : une seule opération SQL à la fois sur la connexion unique
 */
public class MyDatabase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/jobnest" +
                    "?useSSL=false" +
                    "&serverTimezone=UTC" +
                    "&allowPublicKeyRetrieval=true" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8";

    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static MyDatabase instance;
    private Connection conn;

    // ─── SINGLETON ────────────────────────────────────────────────────────────
    private MyDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver introuvable : " + e.getMessage());
        }
        connect();
        startKeepAlive();
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    // ─── CONNEXION ────────────────────────────────────────────────────────────
    // ⚠️ Appelée uniquement depuis des blocs synchronized(this)
    private void connect() {
        try {
            // Fermer proprement l'ancienne connexion si elle existe
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) { }
            }
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] ✅ Connexion établie.");
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Connexion échouée : " + e.getMessage());
        }
    }

    /**
     * Retourne la connexion valide.
     * synchronized → une seule opération SQL à la fois.
     * Le KeepAlive doit attendre la fin de cette méthode avant de pinger.
     */
    public synchronized Connection getConn() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                System.out.println("[DB] Reconnexion nécessaire...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur vérification : " + e.getMessage());
            connect();
        }
        return conn;
    }

    // ─── KEEP-ALIVE ───────────────────────────────────────────────────────────
    /**
     * Ping toutes les 25 secondes.
     * synchronized(MyDatabase.this) → attend la fin de toute requête SQL en cours
     * avant d'envoyer SELECT 1 → plus de conflit sur le ResultSet.
     */
    private void startKeepAlive() {
        Thread keepAlive = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(25_000);   // ping toutes les 25s
                    synchronized (MyDatabase.this) {
                        if (conn != null && !conn.isClosed()) {
                            try (Statement st = conn.createStatement()) {
                                st.execute("SELECT 1");
                                // ping silencieux — pas de log pour ne pas polluer la console
                            }
                        } else {
                            connect();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (SQLException e) {
                    System.err.println("[DB] KeepAlive — reconnexion : " + e.getMessage());
                    synchronized (MyDatabase.this) {
                        connect();
                    }
                }
            }
        });
        keepAlive.setDaemon(true);
        keepAlive.setName("DB-KeepAlive");
        keepAlive.start();
        System.out.println("[DB] ✅ KeepAlive démarré (ping toutes les 25s).");
    }
}