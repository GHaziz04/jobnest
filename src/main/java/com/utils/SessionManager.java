package com.utils;

public class SessionManager {

    private static SessionManager instance;

    private int id;
    private String prenom;
    private String nom;
    private String email;
    private String role;

    private SessionManager() {}

    // ✅ GET INSTANCE
    public static SessionManager getInstance() {
        return instance;
    }

    // ✅ LOGIN
    public static void startSession(
            int id, String prenom, String nom, String email, String role) {

        instance = new SessionManager();
        instance.id = id;
        instance.prenom = prenom;
        instance.nom = nom;
        instance.email = email;
        instance.role = role;
    }

    // ✅ LOGOUT
    public static void clearSession() {
        instance = null;
    }

    // ✅ CHECK IF LOGGED IN
    public static boolean isLoggedIn() {
        return instance != null && instance.email != null;
    }

    // ===== GETTERS =====
    public int getId() {
        return id;
    }

    public int getUserId() {
        return id;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    // ===== SETTERS =====
    public void setId(int id) {
        this.id = id;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }
}