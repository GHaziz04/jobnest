package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Version simplifiée : otp_secret retiré de toutes les opérations
 *    (la colonne reste en DB mais n'est plus utilisée par l'application)
 */
public class CompetenceService {

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    public List<Competence> getAll() {
        List<Competence> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM competence")) {
            while (rs.next()) {
                list.add(new Competence(
                        rs.getInt("id_competence"),
                        rs.getString("nom"),
                        rs.getString("categorie"),
                        rs.getString("description"),
                        rs.getString("niveau_requis")
                        // otp_secret ignoré volontairement
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void ajouter(Competence c) {
        String sql = "INSERT INTO competence (nom, categorie, description, niveau_requis) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getCategorie());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getNiveauRequis());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) c.setIdCompetence(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM competence WHERE id_competence=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Competence c) {
        String sql = "UPDATE competence SET nom=?, categorie=?, description=?, niveau_requis=? WHERE id_competence=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getCategorie());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getNiveauRequis());
            ps.setInt(5, c.getIdCompetence());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Competence findOrCreate(String skillName) {
        if (skillName == null || skillName.trim().isEmpty()) return null;
        String cleaned = skillName.trim().toLowerCase();
        try {
            PreparedStatement check = getConn().prepareStatement(
                    "SELECT * FROM competence WHERE LOWER(nom) = ?");
            check.setString(1, cleaned);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                Competence c = new Competence();
                c.setIdCompetence(rs.getInt("id_competence"));
                c.setNom(rs.getString("nom"));
                c.setCategorie(rs.getString("categorie"));
                c.setNiveauRequis(rs.getString("niveau_requis"));
                return c;
            }
            PreparedStatement ins = getConn().prepareStatement(
                    "INSERT INTO competence (nom, categorie, niveau_requis) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ins.setString(1, skillName);
            ins.setString(2, "technique");
            ins.setString(3, "intermediaire");
            ins.executeUpdate();
            ResultSet keys = ins.getGeneratedKeys();
            if (keys.next()) {
                Competence c = new Competence();
                c.setIdCompetence(keys.getInt(1));
                c.setNom(skillName);
                c.setCategorie("technique");
                c.setNiveauRequis("intermediaire");
                return c;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}