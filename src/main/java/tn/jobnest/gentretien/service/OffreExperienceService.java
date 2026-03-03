package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.dao.OffreExperienceDAO;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;

import java.sql.SQLException;
import java.util.List;

public class OffreExperienceService {

    private OffreExperienceDAO dao = new OffreExperienceDAO();

    // 🔗 Ajouter relation simple
    public void addExperienceToOffre(int idOffre, int idExperience) throws SQLException {

        if (idOffre <= 0 || idExperience <= 0) {
            throw new SQLException("Invalid idOffre or idExperience (must be > 0)");
        }

        dao.addExperienceToOffre(idOffre, idExperience);
    }

    // 📄 Get experiences
    public List<Experience> getExperiencesByOffre(int idOffre) throws SQLException {

        if (idOffre <= 0) {
            throw new SQLException("Invalid idOffre");
        }

        return dao.getExperiencesByOffre(idOffre);
    }

    // ❌ Remove all relations
    public void removeAllExperiencesFromOffre(int idOffre) throws SQLException {

        if (idOffre <= 0) {
            throw new SQLException("Invalid idOffre");
        }

        dao.removeAllExperiencesFromOffre(idOffre);
    }

    // 🔥 SAVE LIST (IMPORTANT FIX)
    public void saveExperiences(OffreEmploi offre) throws SQLException {

        if (offre == null || offre.getIdOffre() <= 0) {
            throw new SQLException("Invalid OffreEmploi");
        }

        // نحذف القديم
        removeAllExperiencesFromOffre(offre.getIdOffre());

        // إذا ما فماش experiences نخرج
        if (offre.getExperiences() == null) return;

        // نرجع ندخل الجديد
        for (Experience exp : offre.getExperiences()) {

            if (exp.getIdExperience() > 0) {
                addExperienceToOffre(offre.getIdOffre(), exp.getIdExperience());
            }
        }
    }
}
