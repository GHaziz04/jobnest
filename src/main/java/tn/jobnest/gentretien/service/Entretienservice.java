package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VERSION FINALE CORRIGÉE
 * ✅ Pas de champ Connection statique
 * ✅ Chaque bloc SQL utilise un try-with-resources sur conn()
 * ✅ MyDatabase.getConn() est synchronized → pas de conflit avec KeepAlive
 */
public class Entretienservice implements Icrud<Entretien> {

    /**
     * Retourne toujours la connexion valide.
     * MyDatabase.getConn() est synchronized → thread-safe avec le KeepAlive.
     */
    private Connection conn() {
        return MyDatabase.getInstance().getConn();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AJOUTER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void ajouter(Entretien entretien) throws SQLException {
        ajouterEtRetournerId(entretien);
    }

    public synchronized int ajouterEtRetournerId(Entretien entretien) throws SQLException {
        String sql =
                "INSERT INTO entretien " +
                        "(date_entretien, heure_debut, heure_fin, type_entretien, lieu, lien_visio, " +
                        " statut, note_recruteur, date_creation, id_recruteur, id_offre, google_event_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate     (1,  entretien.getDateEntretien());
            ps.setTime     (2,  entretien.getHeureDebut());
            ps.setTime     (3,  entretien.getHeureFin());
            ps.setString   (4,  entretien.getTypeEntretien());
            ps.setString   (5,  entretien.getLieu()          != null ? entretien.getLieu()          : "");
            ps.setString   (6,  entretien.getLienVisio()     != null ? entretien.getLienVisio()     : "");
            ps.setString   (7,  entretien.getStatut());
            ps.setString   (8,  entretien.getNoteRecruteur() != null ? entretien.getNoteRecruteur() : "");
            ps.setTimestamp(9,  entretien.getDateCreation());
            ps.setInt      (10, entretien.getIdRecruteur());
            ps.setInt      (11, entretien.getIdOffre());
            ps.setString   (12, entretien.getGoogleEventId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AJOUTER PARTICIPANT
    // ─────────────────────────────────────────────────────────────────────────
    public synchronized boolean ajouterParticipant(int idEntretien, int idCandidat) throws SQLException {
        String sql = "INSERT INTO participant_entretien (id_candidat, id_entretien) VALUES (?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            ps.setInt(2, idEntretien);
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public synchronized void update(Entretien entretien) throws SQLException {
        String sql =
                "UPDATE entretien SET date_entretien=?, heure_debut=?, heure_fin=?, " +
                        "type_entretien=?, lieu=?, lien_visio=?, statut=?, note_recruteur=?, " +
                        "date_creation=?, id_recruteur=?, id_offre=?, google_event_id=? " +
                        "WHERE id_entretien=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDate     (1,  entretien.getDateEntretien());
            ps.setTime     (2,  entretien.getHeureDebut());
            ps.setTime     (3,  entretien.getHeureFin());
            ps.setString   (4,  entretien.getTypeEntretien());
            ps.setString   (5,  entretien.getLieu()          != null ? entretien.getLieu()          : "");
            ps.setString   (6,  entretien.getLienVisio()     != null ? entretien.getLienVisio()     : "");
            ps.setString   (7,  entretien.getStatut());
            ps.setString   (8,  entretien.getNoteRecruteur() != null ? entretien.getNoteRecruteur() : "");
            ps.setTimestamp(9,  entretien.getDateCreation());
            ps.setInt      (10, entretien.getIdRecruteur());
            ps.setInt      (11, entretien.getIdOffre());
            ps.setString   (12, entretien.getGoogleEventId());
            ps.setInt      (13, entretien.getIdEntretien());
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public synchronized void delete(int id) throws SQLException {
        String googleEventId = null;
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT google_event_id FROM entretien WHERE id_entretien = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) googleEventId = rs.getString("google_event_id");
            }
        }
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM participant_entretien WHERE id_entretien = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM entretien WHERE id_entretien = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        if (googleEventId != null && !googleEventId.isBlank()) {
            GoogleCalendarReminderService.supprimerEvenement(googleEventId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AFFICHER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public synchronized List<Entretien> afficher() throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_recruteur = 1";
        // On prépare ET on exécute dans le même bloc synchronized
        // pour que le KeepAlive ne puisse pas interférer pendant le parcours du ResultSet
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) entretiens.add(mapRow(rs));
        }
        return entretiens;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private Entretien mapRow(ResultSet rs) throws SQLException {
        Entretien e = new Entretien();
        e.setIdEntretien  (rs.getInt      ("id_entretien"));
        e.setDateEntretien(rs.getDate     ("date_entretien"));
        e.setHeureDebut   (rs.getTime     ("heure_debut"));
        e.setHeureFin     (rs.getTime     ("heure_fin"));
        e.setTypeEntretien(rs.getString   ("type_entretien"));
        e.setLieu         (rs.getString   ("lieu"));
        e.setLienVisio    (rs.getString   ("lien_visio"));
        e.setStatut       (rs.getString   ("statut"));
        e.setNoteRecruteur(rs.getString   ("note_recruteur"));
        e.setDateCreation (rs.getTimestamp("date_creation"));
        e.setIdRecruteur  (rs.getInt      ("id_recruteur"));
        e.setIdOffre      (rs.getInt      ("id_offre"));
        e.setGoogleEventId(rs.getString   ("google_event_id"));
        return e;
    }

    public synchronized String getOffreTitre(int idOffre) throws SQLException {
        String sql = "SELECT titre FROM offre_emploi WHERE id_offre = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("titre");
            }
        }
        return "Offre inconnue";
    }

    public synchronized List<String> getParticipantEmails(int idEntretien) throws SQLException {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT c.email FROM candidat c " +
                "JOIN participant_entretien p ON c.id_user = p.id_candidat " +
                "WHERE p.id_entretien = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idEntretien);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) emails.add(rs.getString("email"));
            }
        }
        return emails;
    }

    public synchronized List<String> getParticipants(int idEntretien) throws SQLException {
        List<String> participants = new ArrayList<>();
        String sql =
                "SELECT c.prenom, c.nom " +
                        "FROM candidat c " +
                        "JOIN participant_entretien p ON c.id_user = p.id_candidat " +
                        "WHERE p.id_entretien = ? " +
                        "ORDER BY c.prenom, c.nom";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idEntretien);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    participants.add(rs.getString("prenom") + " " + rs.getString("nom"));
            }
        }
        return participants;
    }

    public synchronized List<Entretien> getByRecruteur(int idRecruteur) throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_recruteur = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idRecruteur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) entretiens.add(mapRow(rs));
            }
        }
        return entretiens;
    }

    public synchronized List<Entretien> getEntretiensParOffre(int idOffre) throws SQLException {
        List<Entretien> liste = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_offre = ? ORDER BY date_entretien, heure_debut";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapRow(rs));
            }
        }
        return liste;
    }

    public synchronized boolean hasTimeConflict(Entretien entretien) throws SQLException {
        String sql =
                "SELECT COUNT(*) FROM entretien " +
                        "WHERE id_recruteur = ? AND date_entretien = ? AND id_entretien != ? " +
                        "AND (heure_debut < ? AND heure_fin > ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt (1, entretien.getIdRecruteur());
            ps.setDate(2, entretien.getDateEntretien());
            ps.setInt (3, entretien.getIdEntretien());
            ps.setTime(4, entretien.getHeureFin());
            ps.setTime(5, entretien.getHeureDebut());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public synchronized int annulerEntretiensExpires() throws SQLException {
        String sql =
                "UPDATE entretien " +
                        "SET statut = 'annulé' " +
                        "WHERE statut IN ('proposé', 'confirmé') " +
                        "AND date_entretien < CURDATE()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }
}