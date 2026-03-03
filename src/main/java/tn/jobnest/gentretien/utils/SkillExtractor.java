package tn.jobnest.gentretien.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaire pour extraire les compétences techniques depuis une description textuelle.
 * Fonctionne sans dépendance externe (pas besoin d'API).
 */
public class SkillExtractor {

    // ======================================================
    // DICTIONNAIRE DE COMPÉTENCES TECHNIQUES CONNUES
    // ======================================================
    private static final Set<String> KNOWN_SKILLS = new HashSet<>(Arrays.asList(
            // Langages
            "java", "python", "javascript", "typescript", "kotlin", "swift",
            "c", "c++", "c#", "php", "ruby", "go", "rust", "scala", "dart",
            "r", "matlab", "perl", "bash", "shell",

            // Web Frontend
            "html", "css", "react", "angular", "vue", "vuejs", "reactjs",
            "bootstrap", "tailwind", "sass", "less", "jquery", "svelte",

            // Web Backend
            "spring", "spring boot", "django", "flask", "laravel", "symfony",
            "express", "nodejs", "fastapi", "rails", "asp.net", "quarkus",

            // Mobile
            "android", "ios", "flutter", "react native", "xamarin",

            // Bases de données
            "mysql", "postgresql", "mongodb", "oracle", "sqlite", "redis",
            "elasticsearch", "cassandra", "mariadb", "sql server", "firebase",

            // DevOps / Cloud
            "docker", "kubernetes", "jenkins", "git", "github", "gitlab",
            "aws", "azure", "gcp", "terraform", "ansible", "ci/cd", "linux",

            // Data / AI
            "tensorflow", "pytorch", "keras", "scikit-learn", "pandas",
            "numpy", "machine learning", "deep learning", "nlp", "ai",
            "data science", "spark", "hadoop", "power bi", "tableau",

            // Méthodes / Outils
            "agile", "scrum", "jira", "maven", "gradle", "junit",
            "rest", "graphql", "microservices", "kafka", "rabbitmq",

            // Autres
            "javafx", "hibernate", "jpa", "lombok", "openapi", "swagger"
    ));

    /**
     * Extrait les compétences détectées dans une description.
     *
     * @param description texte libre (description de poste)
     * @return ensemble de compétences trouvées (en minuscules)
     */
    public static Set<String> extractSkills(String description) {

        Set<String> found = new HashSet<>();

        if (description == null || description.trim().isEmpty()) {
            return found;
        }

        String text = description.toLowerCase();

        for (String skill : KNOWN_SKILLS) {
            // Matching mot entier (word boundary)
            Pattern p = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b");
            Matcher m = p.matcher(text);
            if (m.find()) {
                found.add(skill);
            }
        }

        return found;
    }
}