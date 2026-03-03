package tn.jobnest.gentretien.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            "nous","vous","avec","dans","pour","une","des","les","the",
            "and","or","du","de","la","le","un","d","l","en","sur",
            "poste","profil","mission","experience","travail","équipe"
    );

    public static Set<String> extractSkills(String description) {

        Set<String> skills = new HashSet<>();

        description = description.toLowerCase();

        Pattern pattern = Pattern.compile("\\b[a-zA-Z+#.]{3,}\\b");
        Matcher matcher = pattern.matcher(description);

        while (matcher.find()) {
            String word = matcher.group();

            if (!STOP_WORDS.contains(word)) {
                skills.add(capitalize(word));
            }
        }

        return skills;
    }

    private static String capitalize(String word) {
        return word.substring(0,1).toUpperCase() + word.substring(1);
    }
}