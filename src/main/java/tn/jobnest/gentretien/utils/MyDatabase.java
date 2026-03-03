package tn.jobnest.gentretien.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ✅ SOLUTION DÉFINITIVE
 *
 * PROBLÈME : MariaDB ferme la connexion après wait_timeout d'inactivité.
 * Les services qui stockaient "private final Connection cnx = MyDatabase.getInstance().getConn()"
 * gardaient une référence à une connexion MORTE.
 *
 * SOLUTION :
 *  1. Une seule connexion persistante dans le singleton.
 *  2. Thread KeepAlive → envoie "SELECT 1" toutes les 20s → connexion jamais fermée par MariaDB.
 *  3. getConn() vérifie isValid() comme filet de sécurité.
 *  4. TOUS les services appellent MyDatabase.getInstance().getConn() directement,
 *     SANS stocker la connexion dans un champ.
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

    // ─── SINGLETON ───────────────────────────────────────────────────────────
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

    // ─── CONNEXION ───────────────────────────────────────────────────────────
    private void connect() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] ✅ Connexion établie.");
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Connexion échouée : " + e.getMessage());
        }
    }

    /**
     * ✅ Retourne la connexion unique.
     * Reconnecte silencieusement si elle a été fermée (filet de sécurité).
     */
    public synchronized Connection getConn() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return conn;
    }

    // ─── KEEP-ALIVE ──────────────────────────────────────────────────────────
    /**
     * Envoie "SELECT 1" toutes les 20 secondes pour empêcher MariaDB
     * de fermer la connexion inactive (wait_timeout).
     * Le thread est daemon → se ferme automatiquement avec l'application.
     */
    private void startKeepAlive() {
        Thread keepAlive = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(20_000);
                    synchronized (MyDatabase.this) {
                        if (conn != null && !conn.isClosed()) {
                            try (Statement st = conn.createStatement()) {
                                st.execute("SELECT 1");
                            }
                        } else {
                            connect();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (SQLException e) {
                    System.err.println("[DB] KeepAlive - reconnexion : " + e.getMessage());
                    connect();
                }
            }
        });
        keepAlive.setDaemon(true);
        keepAlive.setName("DB-KeepAlive");
        keepAlive.start();
        System.out.println("[DB] ✅ KeepAlive démarré (ping toutes les 20s).");
    }
}