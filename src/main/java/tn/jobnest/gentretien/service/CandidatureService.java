package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.model.Document;
import tn.jobnest.gentretien.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    public List<CandidatureDTO> getCandidaturesPourRecruteur(int idRecruteur) {
        List<CandidatureDTO> liste = new ArrayList<>();
        String sql = "SELECT c.id_candidature, can.id_user, can.nom, can.prenom, can.Titre_pro, " +
                "o.titre, c.statut, c.is_boosted " +
                "FROM candidature c " +
                "JOIN offre_emploi o ON c.id_offre = o.id_offre " +
                "JOIN candidat can ON c.id_candidat = can.id_user " +
                "WHERE o.id_recruteur = ? " +
                "AND (c.statut IS NULL OR c.statut != 'annulé') " +
                "ORDER BY c.is_boosted DESC";

        Connection cnx = MyDatabase.getInstance().getConn();
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = MyDatabase.getInstance().getConn();
            }
            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setInt(1, idRecruteur);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        liste.add(new CandidatureDTO(
                                rs.getInt("id_candidature"),
                                rs.getInt("id_user"),
                                rs.getString("nom") + " " + rs.getString("prenom"),
                                rs.getString("titre"),
                                rs.getString("Titre_pro"),
                                rs.getString("statut"),
                                rs.getInt("is_boosted") == 1
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL getCandidaturesPourRecruteur : " + e.getMessage());
        }
        return liste;
    }

    public boolean modifierStatut(int idCandidature, String nouveauStatut) {
        String sql = "UPDATE candidature SET statut = ? WHERE id_candidature = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, idCandidature);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur SQL modifierStatut : " + e.getMessage());
            return false;
        }
    }

    /**
     * Annule une candidature — elle disparaît de l'interface mais reste en BD.
     */
    public boolean annulerCandidature(int idCandidature) {
        return modifierStatut(idCandidature, "annulé");
    }

    /**
     * Vérifie si un entretien existe déjà pour ce candidat sur cette offre.
     * Une candidature = un seul entretien maximum.
     */
    public boolean entretienDejaExiste(int idCandidat, int idOffre) {
        String sql = "SELECT COUNT(*) FROM participant_entretien pe " +
                "JOIN entretien e ON pe.id_entretien = e.id_entretien " +
                "WHERE pe.id_candidat = ? AND e.id_offre = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCandidat);
            pst.setInt(2, idOffre);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL entretienDejaExiste : " + e.getMessage());
        }
        return false;
    }

    /**
     * Récupère l'id_offre associé à une candidature.
     * Utilisé pour pré-remplir l'entretien avec la bonne offre.
     */
    public int getIdOffreByCandidature(int idCandidature) {
        String sql = "SELECT id_offre FROM candidature WHERE id_candidature = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCandidature);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_offre");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL getIdOffreByCandidature : " + e.getMessage());
        }
        return 0;
    }

    public List<Document> getDocumentsByCandidature(int idCandidature) {
        List<Document> listeDocuments = new ArrayList<>();
        String sql = "SELECT id_document, id_candidature, nom_document, type_document, " +
                "chemin_fichier, date_upload " +
                "FROM document_candidature WHERE id_candidature = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCandidature);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Document doc = new Document(
                            rs.getInt("id_document"),
                            rs.getInt("id_candidature"),
                            rs.getString("nom_document"),
                            rs.getString("type_document"),
                            rs.getString("chemin_fichier"),
                            rs.getTimestamp("date_upload")
                    );
                    listeDocuments.add(doc);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération documents : " + e.getMessage());
        }
        return listeDocuments;
    }
}