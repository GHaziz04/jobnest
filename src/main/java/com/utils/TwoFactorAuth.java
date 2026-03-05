package com.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TwoFactorAuth {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // ── Générer une nouvelle clé secrète TOTP ──────────────────────
    public static GoogleAuthenticatorKey generateSecret() {
        return gAuth.createCredentials();
    }

    // ── Générer l'URL du QR Code (pour Google Authenticator) ───────
    public static String getQRCodeUrl(String email, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "JobNest",   // nom de l'app
                email,       // compte affiché dans l'app
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    // ── Vérifier le code TOTP saisi par l'utilisateur ──────────────
    public static boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    // ── Sauvegarder le secret en DB + activer 2FA ──────────────────
    public static boolean enableTwoFA(int userId, String secret) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET totp_secret = ?, two_fa_enabled = TRUE WHERE id_user = ?");
            ps.setString(1, secret);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Désactiver 2FA ─────────────────────────────────────────────
    public static boolean disableTwoFA(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET totp_secret = NULL, two_fa_enabled = FALSE WHERE id_user = ?");
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Récupérer le secret depuis la DB ───────────────────────────
    public static String getSecret(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT totp_secret FROM users WHERE id_user = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("totp_secret");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── Vérifier si 2FA est activé ─────────────────────────────────
    public static boolean isTwoFAEnabled(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT two_fa_enabled FROM users WHERE id_user = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("two_fa_enabled");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}