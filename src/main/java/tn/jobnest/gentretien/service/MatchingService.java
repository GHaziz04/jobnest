package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════
 *  MatchingService — Calcul du score de matching candidature/offre
 * ════════════════════════════════════════════════════════════════
 *
 *  ALGORITHME :
 *    • Score compétences = (compétences communes / compétences requises par l'offre) × 100
 *    • Score expériences = (expériences communes / expériences requises par l'offre) × 100
 *    • Score final       = (scoreCompetences × 60%) + (scoreExperiences × 40%)
 *
 *  RÈGLES AUTOMATIQUES :
 *    ≥ 80%        → statut "traité"   (acceptée automatiquement)
 *    40% – 79%    → statut "en_revision" (à superviser par le recruteur)
 *    < 40%        → statut "annulé"   (rejetée automatiquement)
 *
 *  SOURCE DES DONNÉES :
 *    • Compétences du candidat : table cv_competence  (nom_competence)
 *    • Expériences du candidat : table cv_experience  (poste + description)
 *    • Compétences de l'offre  : table offre_competence JOIN competence
 *    • Expériences de l'offre  : table offre_experience  JOIN experience
 */
public class MatchingService {

    // ─── Seuils de décision ─────────────────────────────────────
    private static final double SEUIL_AUTO_ACCEPT = 80.0;
    private static final double SEUIL_REVISION    = 40.0;

    // ─── Poids du score final ────────────────────────────────────
    private static final double POIDS_COMPETENCES = 0.60;
    private static final double POIDS_EXPERIENCES  = 0.40;

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    // ════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE : calcule le score ET applique la règle
    // ════════════════════════════════════════════════════════════
    /**
     * Calcule le score de matching pour une candidature donnée,
     * met à jour le statut automatiquement dans la base,
     * et retourne le résultat détaillé.
     *
     * @param idCandidature  l'identifiant de la candidature à évaluer
     * @param idOffre        l'identifiant de l'offre ciblée
     * @param idCandidat     l'identifiant du candidat
     * @return un objet MatchingResult contenant le score et la décision
     */
    public MatchingResult calculerEtAppliquer(int idCandidature, int idOffre, int idCandidat) {

        // 1. Récupérer les listes
        List<String> competencesOffre     = getCompetencesOffre(idOffre);
        List<String> experiencesOffre     = getExperiencesOffre(idOffre);
        List<String> competencesCandidat  = getCompetencesCandidat(idCandidat);
        List<String> experiencesCandidat  = getExperiencesCandidat(idCandidat);

        // 2. Calculer les scores partiels
        double scoreComp = calculerScore(competencesOffre, competencesCandidat);
        double scoreExp  = calculerScore(experiencesOffre, experiencesCandidat);

        // 3. Score pondéré final
        double scoreFinal = (scoreComp * POIDS_COMPETENCES) + (scoreExp * POIDS_EXPERIENCES);
        scoreFinal = Math.round(scoreFinal * 10.0) / 10.0; // arrondi à 1 décimale

        // 4. Déterminer la décision
        String decision;
        String nouveauStatut;
        if (scoreFinal >= SEUIL_AUTO_ACCEPT) {
            decision      = "ACCEPTÉ";
            nouveauStatut = "traité";
        } else if (scoreFinal >= SEUIL_REVISION) {
            decision      = "EN_REVISION";
            nouveauStatut = "en_revision";
        } else {
            decision      = "ANNULÉ";
            nouveauStatut = "annulé";
        }

        // 5. Persister le statut dans la base
        mettreAJourStatutCandidature(idCandidature, nouveauStatut, scoreFinal);

        // 6. Retourner le résultat complet
        return new MatchingResult(
                idCandidature, idOffre, idCandidat,
                scoreFinal, scoreComp, scoreExp,
                decision, nouveauStatut,
                competencesOffre, experiencesOffre,
                competencesCandidat, experiencesCandidat
        );
    }

    // ════════════════════════════════════════════════════════════
    //  SCORE PARTIEL : intersection normalisée
    // ════════════════════════════════════════════════════════════
    /**
     * Calcule le taux de correspondance entre ce que l'offre exige
     * et ce que le candidat possède.
     * Comparaison insensible à la casse avec correspondance partielle.
     */
    private double calculerScore(List<String> exigences, List<String> possession) {
        if (exigences == null || exigences.isEmpty()) return 100.0; // rien requis = parfait
        if (possession == null || possession.isEmpty()) return 0.0;

        int matches = 0;
        for (String req : exigences) {
            String reqNorm = normaliser(req);
            for (String poss : possession) {
                String possNorm = normaliser(poss);
                // Correspondance exacte OU l'un contient l'autre
                if (reqNorm.equals(possNorm)
                        || reqNorm.contains(possNorm)
                        || possNorm.contains(reqNorm)) {
                    matches++;
                    break;
                }
            }
        }
        return Math.round((matches * 100.0 / exigences.size()) * 10.0) / 10.0;
    }

    private String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o");
    }

    // ════════════════════════════════════════════════════════════
    //  LECTURE BD : compétences de l'offre
    // ════════════════════════════════════════════════════════════
    private List<String> getCompetencesOffre(int idOffre) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT c.nom FROM competence c " +
                "JOIN offre_competence oc ON c.id_competence = oc.id_competence " +
                "WHERE oc.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("nom"));
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur compétences offre : " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  LECTURE BD : expériences de l'offre
    // ════════════════════════════════════════════════════════════
    private List<String> getExperiencesOffre(int idOffre) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT e.nom FROM experience e " +
                "JOIN offre_experience oe ON e.id_experience = oe.id_experience " +
                "WHERE oe.id_offre = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("nom"));
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur expériences offre : " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  LECTURE BD : compétences du candidat (table cv_competence)
    // ════════════════════════════════════════════════════════════
    private List<String> getCompetencesCandidat(int idCandidat) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT nom_competence FROM cv_competence WHERE id_candidat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String val = rs.getString("nom_competence");
                    if (val != null && !val.isBlank()) list.add(val);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur compétences candidat : " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  LECTURE BD : expériences du candidat (table cv_experience)
    //  On utilise le poste + la description pour la comparaison
    // ════════════════════════════════════════════════════════════
    private List<String> getExperiencesCandidat(int idCandidat) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT poste, description FROM cv_experience WHERE id_candidat = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String poste = rs.getString("poste");
                    String desc  = rs.getString("description");
                    if (poste != null && !poste.isBlank()) list.add(poste);
                    if (desc  != null && !desc.isBlank())  list.add(desc);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Matching] Erreur expériences candidat : " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  MISE À JOUR BD : statut + score de matching
    // ════════════════════════════════════════════════════════════
    private void mettreAJourStatutCandidature(int idCandidature, String statut, double score) {
        // On sauvegarde le score dans une nouvelle colonne matching_score
        // Si la colonne n'existe pas encore, voir le script SQL de migration ci-dessous
        String sql = "UPDATE candidature SET statut = ?, matching_score = ? WHERE id_candidature = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setDouble(2, score);
            ps.setInt(3, idCandidature);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Fallback : mise à jour sans le score si la colonne n'existe pas encore
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

    // ════════════════════════════════════════════════════════════
    //  CLASSE RÉSULTAT IMBRIQUÉE
    // ════════════════════════════════════════════════════════════
    public static class MatchingResult {
        public final int    idCandidature;
        public final int    idOffre;
        public final int    idCandidat;
        public final double scoreFinal;
        public final double scoreCompetences;
        public final double scoreExperiences;
        public final String decision;       // "ACCEPTÉ" | "EN_REVISION" | "ANNULÉ"
        public final String nouveauStatut;  // "traité"  | "en_revision" | "annulé"
        public final List<String> competencesRequises;
        public final List<String> experiencesRequises;
        public final List<String> competencesCandidat;
        public final List<String> experiencesCandidat;

        public MatchingResult(int idCandidature, int idOffre, int idCandidat,
                              double scoreFinal, double scoreComp, double scoreExp,
                              String decision, String nouveauStatut,
                              List<String> compReq, List<String> expReq,
                              List<String> compCand, List<String> expCand) {
            this.idCandidature      = idCandidature;
            this.idOffre            = idOffre;
            this.idCandidat         = idCandidat;
            this.scoreFinal         = scoreFinal;
            this.scoreCompetences   = scoreComp;
            this.scoreExperiences   = scoreExp;
            this.decision           = decision;
            this.nouveauStatut      = nouveauStatut;
            this.competencesRequises = compReq;
            this.experiencesRequises = expReq;
            this.competencesCandidat = compCand;
            this.experiencesCandidat = expCand;
        }

        /** Retourne true si la candidature a été acceptée automatiquement */
        public boolean estAcceptee()   { return "ACCEPTÉ".equals(decision); }
        /** Retourne true si la candidature nécessite une révision humaine */
        public boolean estEnRevision() { return "EN_REVISION".equals(decision); }
        /** Retourne true si la candidature a été rejetée automatiquement */
        public boolean estAnnulee()    { return "ANNULÉ".equals(decision); }

        @Override
        public String toString() {
            return String.format(
                    "[Matching] Candidature #%d → Score: %.1f%% (Comp: %.1f%% | Exp: %.1f%%) → %s",
                    idCandidature, scoreFinal, scoreCompetences, scoreExperiences, decision);
        }
    }
}