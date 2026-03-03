package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.utils.SkillExtractor;
import tn.jobnest.gentretien.utils.MyDatabase;  // ← importe la bonne classe

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

public class AutoSkillService {

    public void processDescription(String description) {

        Set<String> extractedSkills = SkillExtractor.extractSkills(description);

        if (extractedSkills == null || extractedSkills.isEmpty()) {
            return;
        }

        // Changement unique ici : on utilise MyDatabase au lieu de DatabaseConfig
        try (Connection cnx = MyDatabase.getInstance().getConn()) {

            for (String skill : extractedSkills) {

                if (skill == null || skill.trim().isEmpty()) {
                    continue;
                }

                skill = skill.trim();

                // ✅ CHECK IF SKILL EXISTS (correct column name)
                String checkQuery = "SELECT id_competence FROM competence WHERE LOWER(nom) = LOWER(?)";

                try (PreparedStatement checkPs = cnx.prepareStatement(checkQuery)) {

                    checkPs.setString(1, skill);
                    ResultSet rs = checkPs.executeQuery();

                    // ❌ If skill does NOT exist → insert it
                    if (!rs.next()) {

                        String insert = "INSERT INTO competence(nom, categorie, niveau_requis) VALUES(?, 'technique', 'debutant')";

                        try (PreparedStatement insertPs = cnx.prepareStatement(insert)) {
                            insertPs.setString(1, skill);
                            insertPs.executeUpdate();
                        }

                        System.out.println("Nouvelle compétence ajoutée: " + skill);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}