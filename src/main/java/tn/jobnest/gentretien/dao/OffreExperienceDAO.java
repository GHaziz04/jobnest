package tn.jobnest.gentretien.dao;

import tn.jobnest.gentretien.utils.MyDatabase;
import tn.jobnest.gentretien.model.Experience;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FIX : ne plus stocker "private final Connection connection = ..."
 *    Chaque méthode appelle getConn() directement.
 */
public class OffreExperienceDAO {

    // ✅ PAS de "private final Connection connection" — c'était le bug !
    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    public void addExperienceToOffre(int idOffre, int idExperience) throws SQLException {
        String sql = "INSERT INTO offre_experience (id_offre, id_experience) VALUES (?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idExperience);
            ps.executeUpdate();
        }
    }

    public List<Experience> getExperiencesByOffre(int idOffre) throws SQLException {
        List<Experience> list = new ArrayList<>();
        String sql = "SELECT e.id_experience, e.nom, e.categorie, e.description, e.niveau_requis " +
                "FROM experience e " +
                "INNER JOIN offre_experience oe ON e.id_experience = oe.id_experience " +
                "WHERE oe.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Experience e = new Experience();
                    e.setIdExperience(rs.getInt("id_experience"));
                    e.setNom(rs.getString("nom"));
                    e.setCategorie(rs.getString("categorie"));
                    e.setDescription(rs.getString("description"));
                    e.setNiveauRequis(rs.getString("niveau_requis"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    public void removeAllExperiencesFromOffre(int idOffre) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM offre_experience WHERE id_offre = ?")) {
            ps.setInt(1, idOffre);
            ps.executeUpdate();
        }
    }
}