package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.utils.MyDatabase;  // ← ton singleton

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExperienceService {

    private Connection cnx;

    public ExperienceService() {
        try {
            cnx = MyDatabase.getInstance().getConn();
        } catch (Exception e) {          // ← Exception au lieu de SQLException
            System.err.println("Erreur lors de l'initialisation de la connexion");
            e.printStackTrace();
        }
    }

    // ================= ADD =================
    public void addExperience(Experience e) throws SQLException {

        String sql = "INSERT INTO experience (nom, categorie, description, niveau_requis) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getCategorie());
        ps.setString(3, e.getDescription());
        ps.setString(4, e.getNiveauRequis());

        ps.executeUpdate();
        ps.close();
    }

    // ================= UPDATE =================
    public void updateExperience(Experience e) throws SQLException {

        String sql = "UPDATE experience SET nom=?, categorie=?, description=?, niveau_requis=? WHERE id_experience=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getCategorie());
        ps.setString(3, e.getDescription());
        ps.setString(4, e.getNiveauRequis());
        ps.setInt(5, e.getIdExperience());

        ps.executeUpdate();
        ps.close();
    }

    // ================= DELETE =================
    public void deleteExperience(int id) throws SQLException {

        String sql = "DELETE FROM experience WHERE id_experience=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
        ps.close();
    }

    // ================= GET ALL =================
    public List<Experience> getAll() throws SQLException {

        List<Experience> list = new ArrayList<>();

        String sql = "SELECT * FROM experience";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Experience e = new Experience(
                    rs.getInt("id_experience"),
                    rs.getString("nom"),
                    rs.getString("categorie"),
                    rs.getString("description"),
                    rs.getString("niveau_requis")
            );

            list.add(e);
        }

        rs.close();
        st.close();

        return list;
    }
}