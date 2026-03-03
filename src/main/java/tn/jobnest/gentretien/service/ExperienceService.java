package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExperienceService {

    private final Connection cnx;

    public ExperienceService() {
        cnx = MyDatabase.getInstance().getConn();
    }

    public List<Experience> getAll() {
        List<Experience> list = new ArrayList<>();
        String sql = "SELECT * FROM experience";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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
        String sql = "INSERT INTO experience (nom, categorie, description, niveau_requis) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        String sql = "DELETE FROM experience WHERE id_experience=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Experience e) {
        String sql = "UPDATE experience SET nom=?, categorie=?, description=?, niveau_requis=? WHERE id_experience=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
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