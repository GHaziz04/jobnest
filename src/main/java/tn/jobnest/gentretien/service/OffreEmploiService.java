package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.dao.OffreEmploiDAO;
import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;

import java.sql.SQLException;
import java.util.List;

public class OffreEmploiService {

    private final OffreEmploiDAO dao;
    private final OffreCompetenceService ocService;
    private final OffreExperienceService oeService;
    private final EmailService emailService;

    public OffreEmploiService() {
        this.dao = new OffreEmploiDAO();
        this.ocService = new OffreCompetenceService();
        this.oeService = new OffreExperienceService();
        this.emailService = new EmailService();
    }

    // =====================================================
    // AJOUT OFFRE SIMPLE
    // =====================================================
    public void ajouterOffre(OffreEmploi o) throws SQLException {
        ajouterOffre(o, null, null);
    }

    // =====================================================
    // AJOUT OFFRE COMPLET
    // =====================================================
    public void ajouterOffre(OffreEmploi o,
                             List<Competence> competences,
                             List<Experience> experiences) throws SQLException {

        validateOffre(o);

        int idOffre = dao.ajouterEtRetournerId(o);

        if (idOffre <= 0)
            throw new SQLException("Erreur lors de l'ajout de l'offre.");

        o.setIdOffre(idOffre);

        // Ajout compétences
        if (competences != null) {
            for (Competence c : competences) {
                if (c != null && c.getIdCompetence() > 0) {
                    ocService.addCompetenceToOffre(idOffre, c.getIdCompetence());
                }
            }
        }

        // Ajout expériences
        if (experiences != null) {
            for (Experience e : experiences) {
                if (e != null && e.getIdExperience() > 0) {
                    oeService.addExperienceToOffre(idOffre, e.getIdExperience());
                }
            }
        }

        // 🔥 ENVOI MAIL PROPRE
        sendSafeEmail(
                "Nouvelle offre ajoutée",
                buildAjoutMessage(o)
        );
    }

    // =====================================================
    // GET OFFRES
    // =====================================================
    public List<OffreEmploi> getOffres() throws SQLException {

        List<OffreEmploi> offres = dao.afficher();

        for (OffreEmploi o : offres) {

            if (o == null) continue;

            o.setCompetences(
                    ocService.getCompetencesByOffre(o.getIdOffre())
            );

            o.setExperiences(
                    oeService.getExperiencesByOffre(o.getIdOffre())
            );
        }

        return offres;
    }

    // =====================================================
    // SUPPRIMER
    // =====================================================
    public void supprimerOffre(int id) throws SQLException {

        if (id <= 0)
            throw new IllegalArgumentException("ID invalide !");

        ocService.removeAllCompetencesFromOffre(id);
        oeService.removeAllExperiencesFromOffre(id);

        dao.supprimer(id);

        sendSafeEmail(
                "Offre supprimée",
                buildDeleteMessage(id)
        );
    }

    // =====================================================
    // MODIFIER
    // =====================================================
    public void modifierOffre(OffreEmploi o,
                              List<Competence> competences,
                              List<Experience> experiences) throws SQLException {

        validateOffre(o);

        if (o.getIdOffre() <= 0)
            throw new IllegalArgumentException("ID offre invalide !");

        dao.modifier(o);

        // Reset compétences
        ocService.removeAllCompetencesFromOffre(o.getIdOffre());

        if (competences != null) {
            for (Competence c : competences) {
                if (c != null && c.getIdCompetence() > 0) {
                    ocService.addCompetenceToOffre(
                            o.getIdOffre(),
                            c.getIdCompetence()
                    );
                }
            }
        }

        // Reset expériences
        oeService.removeAllExperiencesFromOffre(o.getIdOffre());

        if (experiences != null) {
            for (Experience e : experiences) {
                if (e != null && e.getIdExperience() > 0) {
                    oeService.addExperienceToOffre(
                            o.getIdOffre(),
                            e.getIdExperience()
                    );
                }
            }
        }

        sendSafeEmail(
                "Offre modifiée",
                buildUpdateMessage(o)
        );
    }

    // =====================================================
    // VALIDATION
    // =====================================================
    private void validateOffre(OffreEmploi o) {

        if (o == null)
            throw new IllegalArgumentException("Offre invalide !");

        if (o.getTitre() == null || o.getTitre().trim().isEmpty())
            throw new IllegalArgumentException("Titre obligatoire !");

        if (o.getEntreprise() == null || o.getEntreprise().trim().isEmpty())
            throw new IllegalArgumentException("Entreprise obligatoire !");
    }

    // =====================================================
    // EMAIL SAFE
    // =====================================================
    private void sendSafeEmail(String subject, String message) {
        try {
            emailService.sendEmail(
                    "wassimchaieb2004@gmail.com",
                    subject,
                    message
            );
        } catch (Exception e) {
            System.out.println("Erreur envoi email : " + e.getMessage());
        }
    }

    // =====================================================
    // MESSAGES TEXTE (PAS HTML)
    // =====================================================
    private String buildAjoutMessage(OffreEmploi o) {

        return "Une nouvelle offre a été publiée.\n\n"
                + "Titre : " + o.getTitre() + "\n"
                + "Entreprise : " + o.getEntreprise() + "\n"
                + "ID : " + o.getIdOffre();
    }

    private String buildUpdateMessage(OffreEmploi o) {

        return "Une offre a été modifiée.\n\n"
                + "Titre : " + o.getTitre() + "\n"
                + "Entreprise : " + o.getEntreprise() + "\n"
                + "ID : " + o.getIdOffre();
    }

    private String buildDeleteMessage(int id) {

        return "Une offre a été supprimée.\n\n"
                + "ID Offre : " + id;
    }
}