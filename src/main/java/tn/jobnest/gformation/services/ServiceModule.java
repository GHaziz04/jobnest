package tn.jobnest.gformation.services;

import tn.jobnest.gformation.model.Module;
import tn.jobnest.gformation.repository.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceModule {
    private Connection cnx;

    public ServiceModule() {
        this.cnx = DataSource.getInstance().getConnection();
    }

    /**
     * Vérifie si la connexion est toujours active, sinon la récupère à nouveau.
     * C'est la solution pour l'erreur "Connection closed".
     */
    private void checkConnection() {
        try {
            if (this.cnx == null || this.cnx.isClosed()) {
                System.out.println("🔄 Reconnexion à la base de données...");
                this.cnx = DataSource.getInstance().getConnection();
            }
        } catch (SQLException e) {
            System.err.println("❌ Impossible de rétablir la connexion : " + e.getMessage());
        }
    }

    public int ajouter(Module m) {
        checkConnection(); // Sécurité
        String sql = "INSERT INTO module (id_formation, titre, ordre, description, type) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getId_formation());
            ps.setString(2, m.getTitre());
            ps.setInt(3, m.getOrdre());
            ps.setString(4, m.getDescription());
            ps.setString(5, m.getType());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout du module : " + e.getMessage());
        }
        return -1;
    }

    public List<Module> getModulesParFormation(int idFormation) {
        checkConnection(); // Sécurité
        List<Module> liste = new ArrayList<>();
        String sql = "SELECT * FROM module WHERE id_formation = ? ORDER BY ordre ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idFormation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Module(
                            rs.getInt("id_module"),
                            rs.getInt("id_formation"),
                            rs.getString("titre"),
                            rs.getInt("ordre"),
                            rs.getString("description"),
                            rs.getString("type")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }
        return liste;
    }

    public void modifier(Module m) {
        checkConnection(); // Sécurité
        String sql = "UPDATE module SET titre = ?, ordre = ?, description = ?, type = ? WHERE id_module = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, m.getTitre());
            ps.setInt(2, m.getOrdre());
            ps.setString(3, m.getDescription());
            ps.setString(4, m.getType());
            ps.setInt(5, m.getId_module());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la modification : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        checkConnection(); // Sécurité
        String sql = "DELETE FROM module WHERE id_module = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    public void updateContenu(int idModule, String nouveauContenu) {
        checkConnection(); // Sécurité
        String sql = "UPDATE module SET description = ? WHERE id_module = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauContenu);
            ps.setInt(2, idModule);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}