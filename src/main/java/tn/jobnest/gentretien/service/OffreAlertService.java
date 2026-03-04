package tn.jobnest.gentretien.service;

import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════
 *  SERVICE D'ALERTES EMAIL POUR LES OFFRES D'EMPLOI
 * ═══════════════════════════════════════════════════════════
 *
 *  CAS 1 — Bientôt fermée, places NON remplies :
 *      date_expiration dans <= 2 jours
 *      ET nb_candidatures < nb_postes
 *      → Email d'avertissement au recruteur créateur de l'offre
 *
 *  CAS 2 — Places remplies, date encore loin :
 *      nb_candidatures >= nb_postes
 *      ET date_expiration > 2 jours
 *      → Email pour suggérer de fermer l'offre maintenant
 *
 *  ✅ L'email ET le nom sont lus directement depuis la table
 *     `recruteur` via id_recruteur — pas de jointure avec users.
 * ═══════════════════════════════════════════════════════════
 */
public class OffreAlertService {

    // Seuil : si expiration dans <= 2 jours → "bientôt fermée"
    private static final int SEUIL_JOURS = 2;

    private final EmailService emailService = new EmailService();

    private Connection getConn() {
        return MyDatabase.getInstance().getConn();
    }

    // ─────────────────────────────────────────────────────────────
    //  POINT D'ENTRÉE PRINCIPAL
    // ─────────────────────────────────────────────────────────────

    /**
     * Vérifie toutes les offres publiées et envoie les alertes email
     * au recruteur qui a créé chaque offre.
     * Appelez cette méthode depuis OffreEmploiController.chargerOffres().
     */
    public void verifierEtEnvoyerAlertes() {
        List<OffreEmploi> offres = getOffresPubilees();
        System.out.println("[OffreAlertService] Vérification de " + offres.size() + " offre(s)...");

        for (OffreEmploi offre : offres) {

            // ✅ Email lu directement depuis la table recruteur
            String emailRecruteur = getEmailRecruteur(offre.getIdRecruteur());
            String nomRecruteur   = getNomRecruteur(offre.getIdRecruteur());

            if (emailRecruteur == null || emailRecruteur.isBlank()) {
                System.err.println("[OffreAlertService] ⚠️ Email introuvable pour recruteur id="
                        + offre.getIdRecruteur() + " — offre ignorée : " + offre.getTitre());
                continue;
            }

            analyserOffre(offre, emailRecruteur, nomRecruteur);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ANALYSE D'UNE OFFRE
    // ─────────────────────────────────────────────────────────────

    private void analyserOffre(OffreEmploi offre,
                               String emailRecruteur,
                               String nomRecruteur) {

        if (offre.getDateExpiration() == null) return;
        if (offre.getNbPostes() <= 0)          return;

        LocalDate today      = LocalDate.now();
        LocalDate expiration = offre.getDateExpiration().toLocalDate();
        long joursRestants   = ChronoUnit.DAYS.between(today, expiration);

        // Offre déjà expirée → ignorer
        if (joursRestants < 0) return;

        int  nbPostes       = offre.getNbPostes();
        int  nbCandidatures = offre.getNbCandidatures();
        boolean placesRemplies = nbCandidatures >= nbPostes;
        boolean bientotFermee  = joursRestants <= SEUIL_JOURS;

        // ── CAS 1 : Bientôt fermée ET places pas encore remplies ──
        if (bientotFermee && !placesRemplies) {
            System.out.println("[CAS 1] \"" + offre.getTitre() + "\" expire dans "
                    + joursRestants + "j — " + nbCandidatures + "/" + nbPostes
                    + " — email → " + emailRecruteur);

            emailService.sendAlerteBientotFermee(
                    emailRecruteur,
                    nomRecruteur,
                    offre.getTitre(),
                    offre.getEntreprise() != null ? offre.getEntreprise() : "—",
                    offre.getDateExpiration().toString(),
                    joursRestants,
                    nbCandidatures,
                    nbPostes
            );
        }

        // ── CAS 2 : Places remplies ET date encore loin ──
        if (placesRemplies && !bientotFermee) {
            System.out.println("[CAS 2] \"" + offre.getTitre() + "\" places remplies "
                    + nbCandidatures + "/" + nbPostes
                    + " — expire dans " + joursRestants + "j"
                    + " — email → " + emailRecruteur);

            emailService.sendAlerteRemplie(
                    emailRecruteur,
                    nomRecruteur,
                    offre.getTitre(),
                    offre.getEntreprise() != null ? offre.getEntreprise() : "—",
                    offre.getDateExpiration().toString(),
                    joursRestants,
                    nbCandidatures,
                    nbPostes
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ✅ RÉCUPÉRER L'EMAIL DU RECRUTEUR
    //  Lecture directe dans la table recruteur (colonne email)
    //  Pas de jointure avec users — c'est là que l'email est stocké
    // ─────────────────────────────────────────────────────────────

    private String getEmailRecruteur(int idRecruteur) {
        String sql = "SELECT email FROM recruteur WHERE id_user = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idRecruteur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            System.err.println("[OffreAlertService] Erreur getEmailRecruteur : " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  ✅ RÉCUPÉRER LE NOM DU RECRUTEUR
    //  Lecture directe dans la table recruteur
    // ─────────────────────────────────────────────────────────────

    private String getNomRecruteur(int idRecruteur) {
        String sql = "SELECT prenom_recruteur, nom_recruteur FROM recruteur WHERE id_user = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idRecruteur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String prenom = rs.getString("prenom_recruteur");
                String nom    = rs.getString("nom_recruteur");
                if (prenom != null && nom != null) return prenom + " " + nom;
                if (prenom != null) return prenom;
                if (nom    != null) return nom;
            }
        } catch (SQLException e) {
            System.err.println("[OffreAlertService] Erreur getNomRecruteur : " + e.getMessage());
        }
        return "Recruteur";
    }

    // ─────────────────────────────────────────────────────────────
    //  RÉCUPÉRER LES OFFRES PUBLIÉES
    // ─────────────────────────────────────────────────────────────

    private List<OffreEmploi> getOffresPubilees() {
        List<OffreEmploi> list = new ArrayList<>();

        String sql =
                "SELECT id_offre, id_recruteur, titre, entreprise, " +
                        "       nb_postes, nb_candidatures, date_expiration, statut " +
                        "FROM offre_emploi " +
                        "WHERE statut = 'publiee' " +
                        "  AND date_expiration IS NOT NULL " +
                        "  AND nb_postes IS NOT NULL " +
                        "  AND nb_postes > 0";

        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                OffreEmploi o = new OffreEmploi();
                o.setIdOffre        (rs.getInt   ("id_offre"));
                o.setIdRecruteur    (rs.getInt   ("id_recruteur"));
                o.setTitre          (rs.getString("titre"));
                o.setEntreprise     (rs.getString("entreprise"));
                o.setNbPostes       (rs.getInt   ("nb_postes"));
                o.setNbCandidatures (rs.getInt   ("nb_candidatures"));
                o.setDateExpiration (rs.getDate  ("date_expiration"));
                o.setStatut         (rs.getString("statut"));
                list.add(o);
            }

        } catch (SQLException e) {
            System.err.println("[OffreAlertService] ❌ Erreur DB : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }
}