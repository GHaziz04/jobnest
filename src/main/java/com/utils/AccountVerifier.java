package com.utils;

import java.sql.*;

public class AccountVerifier {

    /**
     * Active le compte via le token de vérification.
     * @return true si activé avec succès, false si token invalide/expiré
     */
    public static boolean verifyAccount(String token) {
        try (Connection conn = DBConnection.getConnection()) {

            // Vérifier token valide, non expiré, compte encore inactif
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_user FROM users " +
                            "WHERE verification_token = ? " +
                            "AND token_expiration > NOW() " +
                            "AND statut = 'inactif'"
            );
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return false;

            int userId = rs.getInt("id_user");

            // Activer le compte + effacer le token
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET statut = 'actif', " +
                            "verification_token = NULL, " +
                            "token_expiration = NULL " +
                            "WHERE id_user = ?"
            );
            update.setInt(1, userId);
            update.executeUpdate();

            System.out.println("✅ Compte activé pour user ID : " + userId);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}