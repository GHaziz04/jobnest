package tn.jobnest.gformation.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    private static DataSource instance;
    private Connection connection;
    private final String url = "jdbc:mysql://localhost:3306/pidev";
    private final String user = "root";
    private final String password = "";

    private DataSource() {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion à pidev réussie !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public static DataSource getInstance() {
        try {
            if (instance == null || instance.getConnection().isClosed()) {
                instance = new DataSource();
            }
        } catch (SQLException e) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}