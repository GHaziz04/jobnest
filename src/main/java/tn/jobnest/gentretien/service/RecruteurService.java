package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Recruteur;
import java.sql.*;

public class RecruteurService {

    private Connection cnx;

    public RecruteurService() {
        try {
            cnx = DriverManager.getConnection("jdbc:mysql://localhost:3306/jobnest", "root", "");
        } catch (SQLException e) {
            System.err.println("Erreur Connexion BDD : " + e.getMessage());
        }
    }

    /**
     * Récupère un recruteur par son ID.
     */
    public Recruteur getRecruteurById(int idRecruteur) {
        String query = "SELECT * FROM recruteur WHERE id_user = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idRecruteur);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Recruteur(
                        rs.getInt("id_user"),
                        rs.getString("nom_recruteur"),
                        rs.getString("prenom_recruteur"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("adresse"),
                        rs.getString("photo_de_profil"),
                        rs.getString("bio"),
                        rs.getString("git_hub"),
                        rs.getString("statut"),
                        rs.getString("date_inscription"),
                        rs.getString("nom-entreprise"),
                        rs.getString("secteur"),
                        rs.getString("site_web"),
                        rs.getString("description_entreprise"),
                        rs.getDouble("balance")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL getRecruteurById : " + e.getMessage());
        }
        return null;
    }

    /**
     * Met à jour les informations personnelles du recruteur.
     */
    public boolean updateRecruteur(Recruteur r) {
        String query = "UPDATE recruteur SET " +
                "nom_recruteur = ?, prenom_recruteur = ?, email = ?, " +
                "telephone = ?, adresse = ?, bio = ?, git_hub = ?, " +
                "`nom-entreprise` = ?, secteur = ?, site_web = ?, description_entreprise = ?, " +
                "photo_de_profil = ? " +
                "WHERE id_user = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1,  r.getNomRecruteur());
            pst.setString(2,  r.getPrenomRecruteur());
            pst.setString(3,  r.getEmail());
            pst.setString(4,  r.getTelephone());
            pst.setString(5,  r.getAdresse());
            pst.setString(6,  r.getBio());
            pst.setString(7,  r.getGitHub());
            pst.setString(8,  r.getNomEntreprise());
            pst.setString(9,  r.getSecteur());
            pst.setString(10, r.getSiteWeb());
            pst.setString(11, r.getDescriptionEntreprise());
            pst.setString(12, r.getPhotoDeProfil());
            pst.setInt(13,    r.getIdUser());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur SQL updateRecruteur : " + e.getMessage());
            return false;
        }
    }
}