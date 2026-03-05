package com.dao;

import com.model.User;
import com.utils.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    @Override
    public void ajouter(User user) {
        String sql = "INSERT INTO users (nom, prenom, email, mot_de_passe, telephone, adresse, role, statut, email_verifie, date_inscription) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cnx = DBConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getTelephone());
            ps.setString(6, user.getAdresse());
            ps.setString(7, user.getRole());
            ps.setString(8, "actif");
            ps.setBoolean(9, false);
            ps.setDate(10, Date.valueOf(LocalDate.now()));

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public User login(String email, String password) {
        return null; // à faire plus tard
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>();
    }

    @Override
    public void supprimer(User user) {
    }
}
