package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  MatchingService — Algorithme de matching avancé et graduel
 * ════════════════════════════════════════════════════════════════════════════
 */
public class MatchingService {

    private static final double SEUIL_AUTO_ACCEPT = 80.0;
    private static final double SEUIL_REVISION    = 40.0;
    private static final double POIDS_COMPETENCES = 0.60;
    private static final double POIDS_EXPERIENCES = 0.40;

    private static final Map<String, Integer> NIVEAU_ORDRE = new LinkedHashMap<>();
    static {
        NIVEAU_ORDRE.put("debutant",      0);
        NIVEAU_ORDRE.put("intermediaire", 1);
        NIVEAU_ORDRE.put("avance",        2);
        NIVEAU_ORDRE.put("expert",        3);
    }

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════════════════

    public MatchingResult calculerEtAppliquer(int idCandidature, int idOffre, int idCandidat) {

        List<CompetenceOffre>    competencesOffre    = getCompetencesOffre(idOffre);
        List<ExperienceOffre>    experiencesOffre    = getExperiencesOffre(idOffre);
        List<CompetenceCandidat> competencesCandidat = getCompetencesCandidat(idCandidat);
        List<ExperienceCandidat> experiencesCandidat = getExperiencesCandidat(idCandidat);

        ScoreCompetences resComp = calculerScoreCompetences(competencesOffre, competencesCandidat);
        ScoreExperiences resExp  = calculerScoreExperiences(experiencesOffre, experiencesCandidat);

        double scoreFinal = arrondir(
                (resComp.score * POIDS_COMPETENCES) + (resExp.score * POIDS_EXPERIENCES));

        String decision, nouveauStatut;
        if (scoreFinal >= SEUIL_AUTO_ACCEPT) {
            decision = "ACCEPTÉ";   nouveauStatut = "traité";
        } else if (scoreFinal >= SEUIL_REVISION) {
            decision = "EN_REVISION"; nouveauStatut = "en_revision";
        } else {
            decision = "ANNULÉ";    nouveauStatut = "annulé";
        }

        mettreAJourStatut(idCandidature, nouveauStatut, scoreFinal);

        // ── Listes simples pour l'UI ──────────────────────────────────────────
        List<String> compReqDetail  = new ArrayList<>();
        List<String> compCandDetail = new ArrayList<>();
        for (CompetenceOffre co : competencesOffre)
            compReqDetail.add(co.nom + " [" + co.niveauRequis + "]");
        for (CompetenceCandidat cc : competencesCandidat) {
            // ✅ FIX : champ niveauEstime (pas "niveau")
            String niv = cc.niveauEstime != null ? " (" + cc.niveauEstime + ")" : "";
            compCandDetail.add(cc.nom + niv);
        }

        List<String> expReqDetail  = new ArrayList<>();
        List<String> expCandDetail = new ArrayList<>();
        for (ExperienceOffre eo : experiencesOffre) {
            String ans = eo.anneesRequises > 0 ? " [" + eo.anneesRequises + " ans]" : "";
            expReqDetail.add(eo.nom + ans);
        }
        for (ExperienceCandidat ec : experiencesCandidat)
            expCandDetail.add(ec.poste + " @ " + (ec.entreprise != null ? ec.entreprise : "?")
                    + " (" + arrondir(ec.anneesCalculees) + " ans)");

        return new MatchingResult(
                idCandidature, idOffre, idCandidat,
                scoreFinal, arrondir(resComp.score), arrondir(resExp.score),
                decision, nouveauStatut,
                compReqDetail, expReqDetail, compCandDetail, expCandDetail,
                resComp.details, resExp.details);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SCORE COMPÉTENCES
    // ════════════════════════════════════════════════════════════════════════

    private ScoreCompetences calculerScoreCompetences(
            List<CompetenceOffre> requises, List<CompetenceCandidat> possedees) {

        if (requises.isEmpty()) return new ScoreCompetences(100.0, Collections.emptyList());

        List<DetailCompetence> details = new ArrayList<>();
        double totalScore = 0.0;

        for (CompetenceOffre requise : requises) {
            double meilleurScore        = 0.0;
            String nomTrouve            = "—";
            String niveauTrouve         = "—";

            for (CompetenceCandidat possedee : possedees) {
                double scoreNom    = scoreCorrespondanceNom(requise.nom, possedee.nom);
                if (scoreNom == 0.0) continue;
                // ✅ FIX : utiliser niveauEstime (le bon champ)
                double scoreNiveau = calculerScoreNiveau(requise.niveauRequis, possedee.niveauEstime);
                double scorePaire  = scoreNom * scoreNiveau;
                if (scorePaire > meilleurScore) {
                    meilleurScore = scorePaire;
                    nomTrouve     = possedee.nom;
                    // ✅ FIX : utiliser niveauEstime (le bon champ)
                    niveauTrouve  = possedee.niveauEstime != null ? possedee.niveauEstime : "non précisé";
                }
            }
            totalScore += meilleurScore;
            details.add(new DetailCompetence(
                    requise.nom, requise.niveauRequis,
                    nomTrouve, niveauTrouve,
                    arrondir(meilleurScore * 100)));
        }

        return new ScoreCompetences(
                arrondir((totalScore / requises.size()) * 100.0), details);
    }

    private double scoreCorrespondanceNom(String nomRequis, String nomCandidat) {
        if (nomRequis == null || nomCandidat == null) return 0.0;
        String r = normaliser(nomRequis);
        String c = normaliser(nomCandidat);
        if (r.equals(c))                     return 1.0;
        if (r.contains(c) || c.contains(r)) return 0.80;
        String[] motsR = r.split("\\s+");
        String[] motsC = c.split("\\s+");
        int communs = 0;
        for (String mr : motsR)
            for (String mc : motsC)
                if (mr.equals(mc) && mr.length() > 2) communs++;
        int total = Math.max(motsR.length, motsC.length);
        if (total > 0 && communs > 0) return 0.60 * ((double) communs / total);
        return 0.0;
    }

    private double calculerScoreNiveau(String niveauRequis, String niveauCandidat) {
        if (niveauRequis == null || niveauCandidat == null) return 0.85;
        Integer ordreReq  = niveauVersOrdre(niveauRequis);
        Integer ordreCand = niveauVersOrdre(niveauCandidat);
        if (ordreReq == null || ordreCand == null) return 0.85;
        int ecart = ordreReq - ordreCand;
        if (ecart <= 0) return 1.00;
        if (ecart == 1) return 0.75;
        if (ecart == 2) return 0.50;
        return              0.25;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SCORE EXPÉRIENCES
    // ════════════════════════════════════════════════════════════════════════

    private ScoreExperiences calculerScoreExperiences(
            List<ExperienceOffre> requises, List<ExperienceCandidat> possedees) {

        if (requises.isEmpty()) return new ScoreExperiences(100.0, Collections.emptyList());

        List<DetailExperience> details = new ArrayList<>();
        double totalScore = 0.0;

        for (ExperienceOffre requise : requises) {
            double meilleurScore  = 0.0;
            String posteTrouve    = "—";
            double anneesTrouvees = 0.0;

            for (ExperienceCandidat possedee : possedees) {
                double scoreNom    = scoreCorrespondanceNom(requise.nom, possedee.poste);
                if (scoreNom == 0.0) continue;
                double scoreAnnees = calculerScoreAnnees(requise.anneesRequises, possedee.anneesCalculees);
                double scorePaire  = (scoreNom * 0.50) + (scoreAnnees * 0.50);
                if (scorePaire > meilleurScore) {
                    meilleurScore  = scorePaire;
                    posteTrouve    = possedee.poste;
                    anneesTrouvees = possedee.anneesCalculees;
                }
            }
            totalScore += meilleurScore;
            details.add(new DetailExperience(
                    requise.nom, requise.anneesRequises,
                    posteTrouve, arrondir(anneesTrouvees),
                    arrondir(meilleurScore * 100)));
        }

        return new ScoreExperiences(
                arrondir((totalScore / requises.size()) * 100.0), details);
    }

    private double calculerScoreAnnees(double anneesRequises, double anneesCandidat) {
        if (anneesRequises <= 0)             return 1.0;
        if (anneesCandidat <= 0)             return 0.0;
        if (anneesCandidat >= anneesRequises) return 1.0;
        return Math.max(0.0, anneesCandidat / anneesRequises);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LECTURE BASE DE DONNÉES
    // ════════════════════════════════════════════════════════════════════════

    private List<CompetenceOffre> getCompetencesOffre(int idOffre) {
        List<CompetenceOffre> list = new ArrayList<>();
        String sql = "SELECT c.nom, c.niveau_requis FROM competence c " +
                "JOIN offre_competence oc ON c.id_competence = oc.id_competence " +
                "WHERE oc.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new CompetenceOffre(rs.getString("nom"), rs.getString("niveau_requis")));
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur compétences offre : " + e.getMessage());
        }
        return list;
    }

    private List<ExperienceOffre> getExperiencesOffre(int idOffre) {
        List<ExperienceOffre> list = new ArrayList<>();
        String sql = "SELECT e.nom, e.description, e.niveau_requis FROM experience e " +
                "JOIN offre_experience oe ON e.id_experience = oe.id_experience " +
                "WHERE oe.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nom    = rs.getString("nom");
                    String desc   = rs.getString("description");
                    String niveau = rs.getString("niveau_requis");
                    double annees = extraireAnnees(
                            nom + " " + (desc != null ? desc : "") + " " + (niveau != null ? niveau : ""));
                    if (annees <= 0) annees = niveauVersAnnees(niveau);
                    list.add(new ExperienceOffre(nom, annees));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur expériences offre : " + e.getMessage());
        }
        return list;
    }

    private List<CompetenceCandidat> getCompetencesCandidat(int idCandidat) {
        List<CompetenceCandidat> list = new ArrayList<>();
        String sql = "SELECT nom_competence, niveau FROM cv_competence WHERE id_candidat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nom       = rs.getString("nom_competence");
                    // ✅ FIX : getInt + wasNull pour gérer les NULL correctement
                    int    niveauInt = rs.getInt("niveau");
                    boolean estNull  = rs.wasNull();
                    String niveauTexte = estNull ? null : niveauNumeriqueVersTexte(niveauInt);
                    if (nom != null && !nom.isBlank())
                        list.add(new CompetenceCandidat(nom.trim(), niveauTexte));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur compétences candidat : " + e.getMessage());
        }
        return list;
    }

    private List<ExperienceCandidat> getExperiencesCandidat(int idCandidat) {
        List<ExperienceCandidat> list = new ArrayList<>();
        String sql = "SELECT poste, entreprise, date_debut, date_fin, description " +
                "FROM cv_experience WHERE id_candidat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String        poste      = rs.getString("poste");
                    String        entreprise = rs.getString("entreprise");
                    // ✅ FIX : java.sql.Date explicite — résout l'ambiguïté "Date is ambiguous"
                    java.sql.Date debut      = rs.getDate("date_debut");
                    java.sql.Date fin        = rs.getDate("date_fin");
                    String        desc       = rs.getString("description");
                    double        annees     = calculerAnnees(debut, fin);
                    if (poste != null && !poste.isBlank())
                        list.add(new ExperienceCandidat(poste.trim(), entreprise, annees, desc));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur expériences candidat : " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PERSISTANCE
    // ════════════════════════════════════════════════════════════════════════

    private void mettreAJourStatut(int idCandidature, String statut, double score) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "UPDATE candidature SET statut = ?, matching_score = ? WHERE id_candidature = ?")) {
            ps.setString(1, statut);
            ps.setDouble(2, score);
            ps.setInt(3, idCandidature);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Fallback sans matching_score
            try (PreparedStatement ps2 = getConn().prepareStatement(
                    "UPDATE candidature SET statut = ? WHERE id_candidature = ?")) {
                ps2.setString(1, statut);
                ps2.setInt(2, idCandidature);
                ps2.executeUpdate();
            } catch (SQLException ex) {
                System.err.println("[Matching] Erreur MAJ statut : " + ex.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase().trim()
                .replaceAll("[éèêë]", "e").replaceAll("[àâä]", "a")
                .replaceAll("[ùûü]", "u").replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[^a-z0-9#+\\s]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private double extraireAnnees(String texte) {
        if (texte == null) return 0;
        java.util.regex.Matcher mat = java.util.regex.Pattern.compile(
                "(\\d+)(?:\\s*[-à]\\s*(\\d+))?\\s*(?:ans?|annees?|years?)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(texte);
        if (mat.find()) {
            double min = Double.parseDouble(mat.group(1));
            if (mat.group(2) != null) return (min + Double.parseDouble(mat.group(2))) / 2.0;
            return min;
        }
        return 0;
    }

    // ✅ FIX : signature java.sql.Date explicite — élimine l'erreur "Date is ambiguous"
    private double calculerAnnees(java.sql.Date debut, java.sql.Date fin) {
        if (debut == null) return 0;
        LocalDate d = debut.toLocalDate();
        LocalDate f = (fin != null) ? fin.toLocalDate() : LocalDate.now();
        return Math.max(0, ChronoUnit.MONTHS.between(d, f) / 12.0);
    }

    private Integer niveauVersOrdre(String niveau) {
        if (niveau == null) return null;
        return NIVEAU_ORDRE.get(normaliser(niveau));
    }

    private double niveauVersAnnees(String niveau) {
        if (niveau == null) return 0;
        switch (normaliser(niveau)) {
            case "debutant":      return 0.5;
            case "intermediaire": return 2.0;
            case "avance":        return 4.0;
            case "expert":        return 7.0;
            default:              return 0;
        }
    }

    // ✅ FIX : retourne null si niveau == 0 (NULL en BD), évite "cannot find symbol variable niveau"
    private String niveauNumeriqueVersTexte(int niveau) {
        if (niveau <= 0)  return null;
        if (niveau <= 25) return "debutant";
        if (niveau <= 55) return "intermediaire";
        if (niveau <= 80) return "avance";
        return                   "expert";
    }

    private double arrondir(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CLASSES INTERNES
    // ════════════════════════════════════════════════════════════════════════

    private static class CompetenceOffre {
        final String nom, niveauRequis;
        CompetenceOffre(String nom, String niveauRequis) {
            this.nom = nom; this.niveauRequis = niveauRequis;
        }
    }

    private static class ExperienceOffre {
        final String nom;
        final double anneesRequises;
        ExperienceOffre(String nom, double annees) {
            this.nom = nom; this.anneesRequises = annees;
        }
    }

    private static class CompetenceCandidat {
        final String nom;
        // ✅ Le champ s'appelle "niveauEstime" — ne jamais écrire "niveau" seul
        final String niveauEstime;
        CompetenceCandidat(String nom, String niveauEstime) {
            this.nom = nom; this.niveauEstime = niveauEstime;
        }
    }

    private static class ExperienceCandidat {
        final String poste, entreprise, description;
        final double anneesCalculees;
        ExperienceCandidat(String poste, String entreprise, double annees, String desc) {
            this.poste = poste; this.entreprise = entreprise;
            this.anneesCalculees = annees; this.description = desc;
        }
    }

    private static class ScoreCompetences {
        final double score;
        final List<DetailCompetence> details;
        ScoreCompetences(double score, List<DetailCompetence> details) {
            this.score = score; this.details = details;
        }
    }

    private static class ScoreExperiences {
        final double score;
        final List<DetailExperience> details;
        ScoreExperiences(double score, List<DetailExperience> details) {
            this.score = score; this.details = details;
        }
    }

    // ── Détails publics ───────────────────────────────────────────────────────

    public static class DetailCompetence {
        public final String nomRequis, niveauRequis, nomCandidat, niveauCandidat;
        public final double scoreObtenu;
        public DetailCompetence(String nomReq, String nivReq,
                                String nomCand, String nivCand, double score) {
            nomRequis = nomReq; niveauRequis = nivReq;
            nomCandidat = nomCand; niveauCandidat = nivCand; scoreObtenu = score;
        }
        public String toDisplayString() {
            return String.format("%-20s [%s] → %-20s [%s]  =  %.0f%%",
                    nomRequis,   niveauRequis   != null ? niveauRequis   : "?",
                    nomCandidat, niveauCandidat != null ? niveauCandidat : "?",
                    scoreObtenu);
        }
    }

    public static class DetailExperience {
        public final String nomRequis, posteCandidat;
        public final double anneesRequises, anneesCandidat, scoreObtenu;
        public DetailExperience(String nomReq, double ansReq,
                                String posteCand, double ansCand, double score) {
            nomRequis = nomReq; anneesRequises = ansReq;
            posteCandidat = posteCand; anneesCandidat = ansCand; scoreObtenu = score;
        }
        public String toDisplayString() {
            return String.format("%-25s [%.0f ans] → %-25s [%.1f ans]  =  %.0f%%",
                    nomRequis, anneesRequises, posteCandidat, anneesCandidat, scoreObtenu);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RÉSULTAT PUBLIC
    // ════════════════════════════════════════════════════════════════════════

    public static class MatchingResult {
        public final int    idCandidature, idOffre, idCandidat;
        public final double scoreFinal, scoreCompetences, scoreExperiences;
        public final String decision, nouveauStatut;
        public final List<String>            competencesRequises, experiencesRequises;
        public final List<String>            competencesCandidat, experiencesCandidat;
        public final List<DetailCompetence>  detailsCompetences;
        public final List<DetailExperience>  detailsExperiences;

        public MatchingResult(int idC, int idO, int idCand,
                              double sf, double sc, double se,
                              String dec, String stat,
                              List<String> cReq, List<String> eReq,
                              List<String> cCand, List<String> eCand,
                              List<DetailCompetence> dComp,
                              List<DetailExperience> dExp) {
            idCandidature = idC; idOffre = idO; idCandidat = idCand;
            scoreFinal = sf; scoreCompetences = sc; scoreExperiences = se;
            decision = dec; nouveauStatut = stat;
            competencesRequises = cReq; experiencesRequises = eReq;
            competencesCandidat = cCand; experiencesCandidat = eCand;
            detailsCompetences = dComp; detailsExperiences = dExp;
        }

        public boolean estAcceptee()   { return "ACCEPTÉ".equals(decision); }
        public boolean estEnRevision() { return "EN_REVISION".equals(decision); }
        public boolean estAnnulee()    { return "ANNULÉ".equals(decision); }

        @Override
        public String toString() {
            return String.format("[Matching] #%d → %.1f%% (Comp:%.1f%% | Exp:%.1f%%) → %s",
                    idCandidature, scoreFinal, scoreCompetences, scoreExperiences, decision);
        }
    }
}