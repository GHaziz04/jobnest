package tn.jobnest.gformation.services;

import tn.jobnest.gformation.model.Ressource;
import tn.jobnest.gformation.repository.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRessource {
    private Connection cnx;

    public ServiceRessource() {
        // Initialisation de la connexion via le Singleton DataSource
        this.cnx = DataSource.getInstance().getConnection();
    }

    /**
     * Ajoute une ressource et retourne son ID généré (indispensable pour lier le Quiz)
     */
    public int ajouter(Ressource r) throws SQLException {
        String query = "INSERT INTO ressource (titre, valeur_contenu, type, ordre, id_module) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement st = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, r.getTitre());
            st.setString(2, r.getValeur_contenu());
            st.setString(3, r.getType());
            st.setInt(4, r.getOrdre());
            st.setInt(5, r.getId_module());

            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Récupère l'ID auto-incrémenté
                }
            }
        }
        return -1;
    }

    /**
     * Récupère toutes les ressources d'un module
     */
    public List<Ressource> getRessourcesParModule(int idModule) {
        List<Ressource> ressources = new ArrayList<>();
        String sql = "SELECT * FROM ressource WHERE id_module = ? ORDER BY ordre ASC";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idModule);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ressources.add(new Ressource(
                            rs.getInt("id_ressource"),
                            rs.getInt("id_module"),
                            rs.getString("titre"),
                            rs.getString("type"),
                            rs.getString("valeur_contenu"),
                            rs.getInt("ordre")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getRessourcesParModule : " + e.getMessage());
        }
        return ressources;
    }

    /**
     * Modifie une ressource existante
     */
    public void modifier(Ressource r) {
        String sql = "UPDATE ressource SET titre = ?, type = ?, valeur_contenu = ?, ordre = ? WHERE id_ressource = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, r.getTitre());
            pst.setString(2, r.getType());
            pst.setString(3, r.getValeur_contenu());
            pst.setInt(4, r.getOrdre());
            pst.setInt(5, r.getId_ressource());

            pst.executeUpdate();
            System.out.println("✅ Ressource mise à jour !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification ressource : " + e.getMessage());
        }
    }

    /**
     * Supprime une ressource
     */
    public void supprimer(int idRessource) {
        String sql = "DELETE FROM ressource WHERE id_ressource = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idRessource);
            pst.executeUpdate();
            System.out.println("🗑️ Ressource supprimée.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression ressource : " + e.getMessage());
        }
    }
}