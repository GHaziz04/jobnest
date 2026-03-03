package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FIX CRITIQUE :
 * AVANT (bug) : private final Connection cnx = MyDatabase.getInstance().getConn();
 *   → cnx pointe vers une connexion MORTE après timeout MariaDB.
 *
 * APRÈS (corrigé) : chaque méthode appelle getConn() au moment de son exécution.
 *   → utilise toujours la connexion VIVANTE du singleton.
 */
public class ExperienceService {

    // ✅ Méthode privée — appelle le singleton à chaque fois
    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    public List<Experience> getAll() {
        List<Experience> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM experience")) {
            while (rs.next()) {
                list.add(new Experience(
                        rs.getInt("id_experience"),
                        rs.getString("nom"),
                        rs.getString("categorie"),
                        rs.getString("description"),
                        rs.getString("niveau_requis")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addExperience(Experience e) {
        String sql = "INSERT INTO experience (nom, categorie, description, niveau_requis) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getCategorie());
            ps.setString(3, e.getDescription());
            ps.setString(4, e.getNiveauRequis());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) e.setIdExperience(rs.getInt(1));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void supprimer(int id) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM experience WHERE id_experience=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Experience e) {
        String sql = "UPDATE experience SET nom=?, categorie=?, description=?, niveau_requis=? WHERE id_experience=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getCategorie());
            ps.setString(3, e.getDescription());
            ps.setString(4, e.getNiveauRequis());
            ps.setInt(5, e.getIdExperience());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}