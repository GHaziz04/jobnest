package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Entretienservice implements Icrud<Entretien> {
    private final Connection connection = MyDatabase.getInstance().getConn();

    @Override
    public void ajouter(Entretien entretien) throws SQLException {
        String sql = "INSERT INTO entretien (date_entretien, heure_debut, heure_fin, type_entretien, " +
                "lieu, lien_visio, statut, note_recruteur, date_creation, id_recruteur, id_offre) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // RETURN_GENERATED_KEYS pour récupérer l'id_entretien auto-généré
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, entretien.getDateEntretien());
            ps.setTime(2, entretien.getHeureDebut());
            ps.setTime(3, entretien.getHeureFin());
            ps.setString(4, entretien.getTypeEntretien());
            ps.setString(5, entretien.getLieu());
            ps.setString(6, entretien.getLienVisio());
            ps.setString(7, entretien.getStatut());
            ps.setString(8, entretien.getNoteRecruteur());
            ps.setTimestamp(9, entretien.getDateCreation());
            ps.setInt(10, entretien.getIdRecruteur());
            ps.setInt(11, entretien.getIdOffre());
            ps.executeUpdate();
            // Stocker l'ID généré directement dans l'objet entretien
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entretien.setIdEntretien(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Entretien entretien) throws SQLException {
        String sql = "UPDATE entretien SET date_entretien = ?, heure_debut = ?, heure_fin = ?, " +
                "type_entretien = ?, lieu = ?, lien_visio = ?, statut = ?, note_recruteur = ?, " +
                "date_creation = ?, id_recruteur = ?, id_offre = ? WHERE id_entretien = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, entretien.getDateEntretien());
            ps.setTime(2, entretien.getHeureDebut());
            ps.setTime(3, entretien.getHeureFin());
            ps.setString(4, entretien.getTypeEntretien());
            ps.setString(5, entretien.getLieu());
            ps.setString(6, entretien.getLienVisio());
            ps.setString(7, entretien.getStatut());
            ps.setString(8, entretien.getNoteRecruteur());
            ps.setTimestamp(9, entretien.getDateCreation());
            ps.setInt(10, entretien.getIdRecruteur());
            ps.setInt(11, entretien.getIdOffre());
            ps.setInt(12, entretien.getIdEntretien());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        // Supprimer d'abord les participants (contrainte FK)
        String deleteParticipants = "DELETE FROM participant_entretien WHERE id_entretien = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteParticipants)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        String sql = "DELETE FROM entretien WHERE id_entretien = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Entretien> afficher() throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_recruteur = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entretiens.add(mapEntretien(rs));
            }
        }
        return entretiens;
    }

    public List<Entretien> getByRecruteur(int idRecruteur) throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien WHERE id_recruteur = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idRecruteur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entretiens.add(mapEntretien(rs));
                }
            }
        }
        return entretiens;
    }

    private Entretien mapEntretien(ResultSet rs) throws SQLException {
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
        return e;
    }

    public String getOffreTitre(int idOffre) throws SQLException {
        String sql = "SELECT titre FROM offre_emploi WHERE id_offre = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("titre");
            }
        }
        return "Offre inconnue";
    }

    public List<String> getParticipants(int idEntretien) throws SQLException {
        List<String> participants = new ArrayList<>();
        String sql = "SELECT c.prenom, c.nom " +
                "FROM candidat c " +
                "JOIN participant_entretien p ON c.id_user = p.id_candidat " +
                "WHERE p.id_entretien = ? ORDER BY c.prenom, c.nom";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idEntretien);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    participants.add(rs.getString("prenom") + " " + rs.getString("nom"));
                }
            }
        }
        return participants;
    }

    /**
     * Ajoute un candidat comme participant à un entretien.
     * id_participant est AUTO_INCREMENT donc on ne l'insère pas.
     */
    public void ajouterParticipant(int idEntretien, int idCandidat) throws SQLException {
        String sql = "INSERT INTO participant_entretien (id_candidat, id_entretien) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            ps.setInt(2, idEntretien);
            ps.executeUpdate();
        }
    }

    public boolean hasTimeConflict(Entretien entretien) throws SQLException {
        String sql = "SELECT COUNT(*) FROM entretien " +
                "WHERE id_recruteur = ? AND date_entretien = ? AND id_entretien != ? AND (" +
                "  (heure_debut < ? AND heure_fin > ?) OR" +
                "  (heure_debut < ? AND heure_fin > ?) OR" +
                "  (heure_debut >= ? AND heure_fin <= ?) OR" +
                "  (heure_debut <= ? AND heure_fin >= ?)" +
                ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, entretien.getIdRecruteur());
            ps.setDate(2, entretien.getDateEntretien());
            ps.setInt(3, entretien.getIdEntretien());
            ps.setTime(4, entretien.getHeureFin());
            ps.setTime(5, entretien.getHeureDebut());
            ps.setTime(6, entretien.getHeureFin());
            ps.setTime(7, entretien.getHeureDebut());
            ps.setTime(8, entretien.getHeureDebut());
            ps.setTime(9, entretien.getHeureFin());
            ps.setTime(10, entretien.getHeureDebut());
            ps.setTime(11, entretien.getHeureFin());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}