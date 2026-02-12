package tn.jobnest.gentretien.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class MyDatabase {
    private final String url = "jdbc:mysql://localhost:3306/jobnest";
    private final String user = "root";
    private final String password = "";
    private Connection conn;
    private static MyDatabase instance;

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }

    private MyDatabase() {
        try {
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection established");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }
}

