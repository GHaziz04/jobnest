package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.dao.OffreEmploiDAO;
import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.model.Recruteur;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiService {

    private final OffreEmploiDAO   dao              = new OffreEmploiDAO();
    private final RecruteurService recruteurService = new RecruteurService();

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    // ─────────────────────────────────────────────────────────────
    //  ✅ NOM DU RECRUTEUR — utilise RecruteurService existant
    // ─────────────────────────────────────────────────────────────
    public String getNomRecruteur(int idRecruteur) {
        try {
            Recruteur r = recruteurService.getRecruteurById(idRecruteur);
            if (r != null) return r.getNomComplet();
        } catch (Exception e) {
            System.err.println("[RecruteurNom] " + e.getMessage());
        }
        return "Recruteur #" + idRecruteur;
    }

    // ─────────────────────────────────────────────────────────────
    //  GET ALL — avec compétences et expériences
    // ─────────────────────────────────────────────────────────────
    public List<OffreEmploi> getOffres() throws SQLException {
        List<OffreEmploi> offres = dao.afficher();
        for (OffreEmploi o : offres) {
            o.setCompetences(getCompetencesByOffre(o.getIdOffre()));
            o.setExperiences(getExperiencesByOffre(o.getIdOffre()));
        }
        return offres;
    }

    // ─────────────────────────────────────────────────────────────
    //  AJOUTER
    // ─────────────────────────────────────────────────────────────
    public void ajouterOffre(OffreEmploi offre,
                             List<Competence> competences,
                             List<Experience> experiences) throws SQLException {
        int idOffre = dao.ajouterEtRetournerId(offre);
        if (competences != null)
            for (Competence c : competences) addCompetenceToOffre(idOffre, c.getIdCompetence());
        if (experiences != null)
            for (Experience e : experiences) addExperienceToOffre(idOffre, e.getIdExperience());
    }

    // ─────────────────────────────────────────────────────────────
    //  MODIFIER
    // ─────────────────────────────────────────────────────────────
    public void modifierOffre(OffreEmploi offre,
                              List<Competence> competences,
                              List<Experience> experiences) throws SQLException {
        dao.modifier(offre);
        removeAllCompetencesFromOffre(offre.getIdOffre());
        removeAllExperiencesFromOffre(offre.getIdOffre());
        if (competences != null)
            for (Competence c : competences) addCompetenceToOffre(offre.getIdOffre(), c.getIdCompetence());
        if (experiences != null)
            for (Experience e : experiences) addExperienceToOffre(offre.getIdOffre(), e.getIdExperience());
    }

    // ─────────────────────────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────────────────────────
    public void supprimerOffre(int idOffre) throws SQLException {
        removeAllCompetencesFromOffre(idOffre);
        removeAllExperiencesFromOffre(idOffre);
        dao.supprimer(idOffre);
    }

    // ─────────────────────────────────────────────────────────────
    //  FERMER UNE OFFRE
    // ─────────────────────────────────────────────────────────────
    public void fermerOffre(int idOffre) throws SQLException {
        String sql = "UPDATE offre_emploi SET statut = 'fermee', " +
                "date_expiration = CURDATE() " +
                "WHERE id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Fermeture échouée : offre introuvable (id=" + idOffre + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  COMPÉTENCES PAR OFFRE
    // ─────────────────────────────────────────────────────────────
    private List<Competence> getCompetencesByOffre(int idOffre) throws SQLException {
        List<Competence> list = new ArrayList<>();
        String sql = "SELECT c.* FROM competence c " +
                "INNER JOIN offre_competence oc ON c.id_competence = oc.id_competence " +
                "WHERE oc.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Competence c = new Competence();
                c.setIdCompetence(rs.getInt("id_competence"));
                c.setNom(rs.getString("nom"));
                c.setCategorie(rs.getString("categorie"));
                c.setNiveauRequis(rs.getString("niveau_requis"));
                list.add(c);
            }
        }
        return list;
    }

    private void addCompetenceToOffre(int idOffre, int idCompetence) throws SQLException {
        String sql = "INSERT IGNORE INTO offre_competence (id_offre, id_competence) VALUES (?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre); ps.setInt(2, idCompetence);
            ps.executeUpdate();
        }
    }

    private void removeAllCompetencesFromOffre(int idOffre) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM offre_competence WHERE id_offre=?")) {
            ps.setInt(1, idOffre); ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EXPÉRIENCES PAR OFFRE
    // ─────────────────────────────────────────────────────────────
    private List<Experience> getExperiencesByOffre(int idOffre) throws SQLException {
        List<Experience> list = new ArrayList<>();
        String sql = "SELECT e.* FROM experience e " +
                "INNER JOIN offre_experience oe ON e.id_experience = oe.id_experience " +
                "WHERE oe.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Experience e = new Experience();
                e.setIdExperience(rs.getInt("id_experience"));
                e.setNom(rs.getString("nom"));
                e.setCategorie(rs.getString("categorie"));
                e.setNiveauRequis(rs.getString("niveau_requis"));
                list.add(e);
            }
        }
        return list;
    }

    private void addExperienceToOffre(int idOffre, int idExperience) throws SQLException {
        String sql = "INSERT IGNORE INTO offre_experience (id_offre, id_experience) VALUES (?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre); ps.setInt(2, idExperience);
            ps.executeUpdate();
        }
    }

    private void removeAllExperiencesFromOffre(int idOffre) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM offre_experience WHERE id_offre=?")) {
            ps.setInt(1, idOffre); ps.executeUpdate();
        }
    }
}