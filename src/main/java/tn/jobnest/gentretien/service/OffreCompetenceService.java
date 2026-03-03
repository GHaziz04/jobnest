package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.utils.MyDatabase;  // ← ton singleton

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OffreCompetenceService {

    // =================================
    // 1️⃣ AJOUTER COMPETENCE À UNE OFFRE
    // =================================
    public void addCompetenceToOffre(int idOffre, int idCompetence) throws SQLException {

        String sql = """
                INSERT INTO offre_competence (id_offre, id_competence)
                VALUES (?, ?)
                """;

        try (
                Connection cnx = MyDatabase.getInstance().getConn();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idCompetence);
            ps.executeUpdate();
        }
    }

    // =====================================
    // 2️⃣ SUPPRIMER COMPETENCE D’UNE OFFRE
    // =====================================
    public void removeCompetenceFromOffre(int idOffre, int idCompetence) throws SQLException {

        String sql = """
                DELETE FROM offre_competence
                WHERE id_offre = ? AND id_competence = ?
                """;

        try (
                Connection cnx = MyDatabase.getInstance().getConn();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {
            ps.setInt(1, idOffre);
            ps.setInt(2, idCompetence);
            ps.executeUpdate();
        }
    }

    // ======================================
    // 3️⃣ LISTER COMPETENCES D’UNE OFFRE
    // ======================================
    public List<Competence> getCompetencesByOffre(int idOffre) throws SQLException {

        List<Competence> competences = new ArrayList<>();

        String sql = """
                SELECT c.*
                FROM competence c
                JOIN offre_competence oc
                  ON c.id_competence = oc.id_competence
                WHERE oc.id_offre = ?
                """;

        try (
                Connection cnx = MyDatabase.getInstance().getConn();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, idOffre);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Competence c = new Competence(
                        rs.getInt("id_competence"),
                        rs.getString("nom"),
                        rs.getString("categorie"),
                        rs.getString("description"),
                        rs.getString("niveau_requis")
                );
                competences.add(c);
            }
        }

        return competences;
    }

    // ==========================================
    // 4️⃣ SUPPRIMER TOUTES LES COMPETENCES D’UNE OFFRE
    // ==========================================
    public void removeAllCompetencesFromOffre(int idOffre) throws SQLException {

        String sql = """
                DELETE FROM offre_competence
                WHERE id_offre = ?
                """;

        try (
                Connection cnx = MyDatabase.getInstance().getConn();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {
            ps.setInt(1, idOffre);
            ps.executeUpdate();
        }
    }
}