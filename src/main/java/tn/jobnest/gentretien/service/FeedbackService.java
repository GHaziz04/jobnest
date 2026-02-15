package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Feedback;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService implements Icrud<Feedback> {
    private final Connection connection = MyDatabase.getInstance().getConn();

    @Override
    public void ajouter(Feedback feedback) throws SQLException {
        String sql = "INSERT INTO feedback (id_entretien, competence_techniques, competence_communication, " +
                "motivation, adequation_au_poste, commentaire, competence_manquantes, " +
                "suggestion_formation, date_feedback) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, feedback.getIdEntretien());
            ps.setInt(2, feedback.getCompetenceTechniques());
            ps.setInt(3, feedback.getCompetenceCommunication());
            ps.setInt(4, feedback.getMotivation());
            ps.setInt(5, feedback.getAdequationAuPoste());
            ps.setString(6, feedback.getCommentaire());
            ps.setString(7, feedback.getCompetenceManquantes());
            ps.setBoolean(8, feedback.isSuggestionFormation());
            ps.setTimestamp(9, feedback.getDateFeedback());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Feedback feedback) throws SQLException {
        String sql = "UPDATE feedback SET id_entretien = ?, competence_techniques = ?, " +
                "competence_communication = ?, motivation = ?, adequation_au_poste = ?, " +
                "commentaire = ?, competence_manquantes = ?, suggestion_formation = ?, " +
                "date_feedback = ? WHERE id_feedback = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, feedback.getIdEntretien());
            ps.setInt(2, feedback.getCompetenceTechniques());
            ps.setInt(3, feedback.getCompetenceCommunication());
            ps.setInt(4, feedback.getMotivation());
            ps.setInt(5, feedback.getAdequationAuPoste());
            ps.setString(6, feedback.getCommentaire());
            ps.setString(7, feedback.getCompetenceManquantes());
            ps.setBoolean(8, feedback.isSuggestionFormation());
            ps.setTimestamp(9, feedback.getDateFeedback());
            ps.setInt(10, feedback.getIdFeedback());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM feedback WHERE id_feedback = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Feedback> afficher() throws SQLException {
        List<Feedback> feedbacks = new ArrayList<>();
        String sql = "SELECT * FROM feedback";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Feedback f = new Feedback();
                f.setIdFeedback(rs.getInt("id_feedback"));
                f.setIdEntretien(rs.getInt("id_entretien"));
                f.setCompetenceTechniques(rs.getInt("competence_techniques"));
                f.setCompetenceCommunication(rs.getInt("competence_communication"));
                f.setMotivation(rs.getInt("motivation"));
                f.setAdequationAuPoste(rs.getInt("adequation_au_poste"));
                f.setCommentaire(rs.getString("commentaire"));
                f.setCompetenceManquantes(rs.getString("competence_manquantes"));
                f.setSuggestionFormation(rs.getBoolean("suggestion_formation"));
                f.setDateFeedback(rs.getTimestamp("date_feedback"));
                feedbacks.add(f);
            }
        }
        return feedbacks;
    }

    /**
     * Récupère le feedback associé à un entretien spécifique
     */
    public Feedback getFeedbackByEntretien(int idEntretien) throws SQLException {
        String sql = "SELECT * FROM feedback WHERE id_entretien = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idEntretien);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Feedback f = new Feedback();
                    f.setIdFeedback(rs.getInt("id_feedback"));
                    f.setIdEntretien(rs.getInt("id_entretien"));
                    f.setCompetenceTechniques(rs.getInt("competence_techniques"));
                    f.setCompetenceCommunication(rs.getInt("competence_communication"));
                    f.setMotivation(rs.getInt("motivation"));
                    f.setAdequationAuPoste(rs.getInt("adequation_au_poste"));
                    f.setCommentaire(rs.getString("commentaire"));
                    f.setCompetenceManquantes(rs.getString("competence_manquantes"));
                    f.setSuggestionFormation(rs.getBoolean("suggestion_formation"));
                    f.setDateFeedback(rs.getTimestamp("date_feedback"));
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Vérifie si un feedback existe déjà pour cet entretien
     */
    public boolean feedbackExists(int idEntretien) throws SQLException {
        String sql = "SELECT COUNT(*) FROM feedback WHERE id_entretien = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idEntretien);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}