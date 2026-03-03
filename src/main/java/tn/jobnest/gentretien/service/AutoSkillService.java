package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.utils.MyDatabase;
import tn.jobnest.gentretien.utils.SkillExtractor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

/**
 * ✅ FIX : utilise MyDatabase.getInstance().getConn() à chaque opération SQL.
 */
public class AutoSkillService {

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    public void processDescription(String description) {
        Set<String> extractedSkills = SkillExtractor.extractSkills(description);
        if (extractedSkills == null || extractedSkills.isEmpty()) return;

        for (String skill : extractedSkills) {
            if (skill == null || skill.trim().isEmpty()) continue;
            skill = skill.trim();
            try {
                String checkQuery = "SELECT id_competence FROM competence WHERE LOWER(nom) = LOWER(?)";
                try (PreparedStatement checkPs = getConn().prepareStatement(checkQuery)) {
                    checkPs.setString(1, skill);
                    ResultSet rs = checkPs.executeQuery();
                    if (!rs.next()) {
                        String insert = "INSERT INTO competence(nom, categorie, niveau_requis) VALUES(?, 'technique', 'debutant')";
                        try (PreparedStatement insertPs = getConn().prepareStatement(insert)) {
                            insertPs.setString(1, skill);
                            insertPs.executeUpdate();
                            System.out.println("Nouvelle compétence ajoutée: " + skill);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}