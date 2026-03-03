package tn.jobnest.gentretien.dao;



import tn.jobnest.gentretien.utils.MyDatabase;
import tn.jobnest.gentretien.model.Experience;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreExperienceDAO {

    private final Connection connection = MyDatabase.getInstance().getConn();

    // ✅ Ajouter relation offre <-> experience
    public void addExperienceToOffre(int idOffre, int idExperience) throws SQLException {

        String sql = "INSERT INTO offre_experience (id_offre, id_experience) VALUES (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idExperience);
            ps.executeUpdate();
        }
    }

    // ✅ Get experiences by offre
    public List<Experience> getExperiencesByOffre(int idOffre) throws SQLException {

        List<Experience> list = new ArrayList<>();

        String sql = "SELECT e.id_experience, e.nom, e.categorie, e.description, e.niveau_requis " +
                "FROM experience e " +
                "INNER JOIN offre_experience oe ON e.id_experience = oe.id_experience " +
                "WHERE oe.id_offre = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

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

    // ✅ Remove all relations for an offre
    public void removeAllExperiencesFromOffre(int idOffre) throws SQLException {

        String sql = "DELETE FROM offre_experience WHERE id_offre = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.executeUpdate();
        }
    }
}
