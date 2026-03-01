package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.model.Document;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    // ─────────────────────────────────────────────────────────────────────────
    //  RÉCUPÉRER LES CANDIDATURES D'UN RECRUTEUR
    // ─────────────────────────────────────────────────────────────────────────
    public List<CandidatureDTO> getCandidaturesPourRecruteur(int idRecruteur) {
        List<CandidatureDTO> liste = new ArrayList<>();
        String sql =
                "SELECT c.id_candidature, can.id_user, can.nom, can.prenom, can.Titre_pro, " +
                        "o.titre, c.statut, c.is_boosted, o.id_offre " +
                        "FROM candidature c " +
                        "JOIN offre_emploi o ON c.id_offre = o.id_offre " +
                        "JOIN candidat can ON c.id_candidat = can.id_user " +
                        "WHERE o.id_recruteur = ? " +
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
                                rs.getInt("is_boosted") == 1,
                                rs.getInt("id_offre")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL dans CandidatureService : " + e.getMessage());
        }
        return liste;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFIER LE STATUT D'UNE CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    public boolean modifierStatut(int idCandidature, String nouveauStatut) {
        String sql = "UPDATE candidature SET statut = ? WHERE id_candidature = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, idCandidature);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur SQL dans CandidatureService (Update) : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RÉCUPÉRER LES DOCUMENTS D'UNE CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    public List<Document> getDocumentsByCandidature(int idCandidature) {
        List<Document> listeDocuments = new ArrayList<>();
        String sql =
                "SELECT id_document, id_candidature, nom_document, type_document, chemin_fichier, date_upload " +
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

    // ─────────────────────────────────────────────────────────────────────────
    //  VÉRIFIER SI UN CANDIDAT EST DÉJÀ PARTICIPANT D'UN ENTRETIEN
    //  (pour n'importe quel entretien — utilisé dans le badge historique)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean candidatAUnEntretien(int idCandidat) {
        String sql = "SELECT COUNT(*) FROM participant_entretien WHERE id_candidat = ?";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCandidat);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification entretien candidat #" + idCandidat + " : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VÉRIFIER SI CE CANDIDAT A DÉJÀ UN ENTRETIEN POUR CETTE OFFRE PRÉCISE
    //  → Règle : une candidature = un seul entretien
    //  → On vérifie via participant_entretien JOIN entretien ON id_offre
    // ─────────────────────────────────────────────────────────────────────────
    public boolean candidatADejaUnEntretienPourOffre(int idCandidat, int idOffre) {
        String sql =
                "SELECT COUNT(*) FROM participant_entretien pe " +
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
            System.err.println("Erreur vérification entretien candidat/offre : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RÉCUPÉRER LES ENTRETIENS EXISTANTS POUR UNE OFFRE DONNÉE
    //  → Permet au recruteur de choisir de rejoindre un entretien existant
    // ─────────────────────────────────────────────────────────────────────────
    public List<Entretien> getEntretiensPourOffre(int idOffre) {
        List<Entretien> liste = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_offre = ? ORDER BY date_entretien, heure_debut";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idOffre);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Entretien e = new Entretien();
                    e.setIdEntretien(rs.getInt("id_entretien"));
                    e.setDateEntretien(rs.getDate("date_entretien"));
                    e.setHeureDebut(rs.getTime("heure_debut"));
                    e.setHeureFin(rs.getTime("heure_fin"));
                    e.setTypeEntretien(rs.getString("type_entretien"));
                    e.setLieu(rs.getString("lieu"));
                    e.setLienVisio(rs.getString("lien_visio"));
                    e.setStatut(rs.getString("statut"));
                    e.setNoteRecruteur(rs.getString("note_recruteur"));
                    e.setDateCreation(rs.getTimestamp("date_creation"));
                    e.setIdRecruteur(rs.getInt("id_recruteur"));
                    e.setIdOffre(rs.getInt("id_offre"));
                    liste.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération entretiens pour offre #" + idOffre + " : " + e.getMessage());
        }
        return liste;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AJOUTER UN PARTICIPANT À UN ENTRETIEN EXISTANT
    // ─────────────────────────────────────────────────────────────────────────
    public boolean ajouterParticipant(int idEntretien, int idCandidat) {
        String sql = "INSERT INTO participant_entretien (id_candidat, id_entretien) VALUES (?, ?)";
        Connection cnx = MyDatabase.getInstance().getConn();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCandidat);
            pst.setInt(2, idEntretien);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajout participant : " + e.getMessage());
            return false;
        }
    }
}