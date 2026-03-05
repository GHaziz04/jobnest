package com.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PreferencesManager {

    private static PreferencesManager instance;

    private String theme = "light";
    private String langue = "fr";
    private boolean notifEnabled = true;
    private boolean emailEnabled = true;
    private boolean profilVisible = true;
    private boolean twoFaEnabled = false;

    private PreferencesManager() {}

    public static PreferencesManager getInstance() {
        if (instance == null) instance = new PreferencesManager();
        return instance;
    }

    // ===== CHARGER depuis la BDD =====
    public void loadFromDB(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT theme, langue, notif_enabled, email_enabled, profil_visible, two_fa_enabled FROM users WHERE id_user = ?"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                theme         = rs.getString("theme") != null ? rs.getString("theme") : "light";
                langue        = rs.getString("langue") != null ? rs.getString("langue") : "fr";
                notifEnabled  = rs.getInt("notif_enabled") == 1;
                emailEnabled  = rs.getInt("email_enabled") == 1;
                profilVisible = rs.getInt("profil_visible") == 1;
                twoFaEnabled  = rs.getInt("two_fa_enabled") == 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== SAUVEGARDER dans la BDD =====
    public void saveTheme(int userId, String theme) {
        this.theme = theme;
        saveField(userId, "theme", theme);
    }

    public void saveLangue(int userId, String langue) {
        this.langue = langue;
        saveField(userId, "langue", langue);
    }

    public void saveNotif(int userId, boolean val) {
        this.notifEnabled = val;
        saveField(userId, "notif_enabled", val ? 1 : 0);
    }

    public void saveEmail(int userId, boolean val) {
        this.emailEnabled = val;
        saveField(userId, "email_enabled", val ? 1 : 0);
    }

    public void saveProfilVisible(int userId, boolean val) {
        this.profilVisible = val;
        saveField(userId, "profil_visible", val ? 1 : 0);
    }

    public void saveTwoFA(int userId, boolean val) {
        this.twoFaEnabled = val;
        saveField(userId, "two_fa_enabled", val ? 1 : 0);
    }

    private void saveField(int userId, String column, Object value) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET " + column + " = ? WHERE id_user = ?"
            );
            ps.setObject(1, value);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== GETTERS =====
    public String getTheme()       { return theme; }
    public String getLangue()      { return langue; }
    public boolean isNotifEnabled(){ return notifEnabled; }
    public boolean isEmailEnabled(){ return emailEnabled; }
    public boolean isProfilVisible(){ return profilVisible; }
    public boolean isTwoFaEnabled(){ return twoFaEnabled; }
}