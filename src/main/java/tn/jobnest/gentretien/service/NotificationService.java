package tn.jobnest.gentretien.service;

import javafx.application.Platform;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.model.Notification;
import tn.jobnest.gentretien.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service de notifications in-app JobNest — VERSION FINALE
 *
 * Fonctionnalités :
 *  ✅ Rappel 24h avant l'entretien  → toast orange + badge
 *  ✅ Rappel 1h  avant l'entretien  → toast orange vif + badge
 *  ✅ Notification d'annulation automatique → toast rouge + badge
 *  ✅ Persistance en BDD (table notification)
 *  ✅ Déduplication (pas de doublon si l'app est relancée)
 *  ✅ Thread-safe : toutes les requêtes SQL sont synchronized
 */
public class NotificationService {

    // ── Singleton thread-safe ────────────────────────────────────────
    private static volatile NotificationService instance;

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) instance = new NotificationService();
            }
        }
        return instance;
    }

    // ── Scheduler daemon ─────────────────────────────────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jobnest-notif-scheduler");
                t.setDaemon(true);
                return t;
            });

    /**
     * Clés des rappels déjà planifiés → évite les doublons
     * quand rafraichirListe() est appelé plusieurs fois.
     * Format : "idEntretien_TYPE"  ex: "37_RAPPEL_24H"
     */
    private final Set<String> rappelsPlanifies =
            Collections.synchronizedSet(new HashSet<>());

    private volatile Consumer<Notification> onNewNotification;

    private NotificationService() { }

    // ════════════════════════════════════════════════════════════════
    //  CALLBACK UI
    // ════════════════════════════════════════════════════════════════
    public void setOnNewNotification(Consumer<Notification> callback) {
        this.onNewNotification = callback;
    }

    // ════════════════════════════════════════════════════════════════
    //  PLANIFICATION DES RAPPELS 24H ET 1H
    // ════════════════════════════════════════════════════════════════

    /**
     * À appeler après chaque chargement de la liste des entretiens.
     * Idempotent : un entretien déjà planifié ne sera pas replanifié.
     */
    public void planifierRappels(List<Entretien> entretiens) {
        for (Entretien e : entretiens) {
            if (!"proposé".equals(e.getStatut())) continue;
            if (e.getDateEntretien() == null || e.getHeureDebut() == null) continue;

            LocalDateTime debut = LocalDateTime.of(
                    e.getDateEntretien().toLocalDate(),
                    e.getHeureDebut().toLocalTime());

            planifierSiNecessaire(e, debut.minusHours(24),
                    Notification.Type.RAPPEL_24H,
                    "Rappel — Entretien dans 24h",
                    buildMessage24h(e));

            planifierSiNecessaire(e, debut.minusHours(1),
                    Notification.Type.RAPPEL_1H,
                    "Rappel urgent — Entretien dans 1h",
                    buildMessage1h(e));
        }
    }

    private void planifierSiNecessaire(Entretien e,
                                       LocalDateTime moment,
                                       Notification.Type type,
                                       String titre,
                                       String message) {
        String cle = e.getIdEntretien() + "_" + type.name();

        // Déduplication : si déjà planifié dans cette session, on ignore
        if (rappelsPlanifies.contains(cle)) return;

        // Déduplication BDD : si déjà notifié dans une session précédente, on ignore
        try {
            if (notificationExisteEnBDD(e.getIdEntretien(), type)) {
                System.out.println("[NotifService] " + type.name()
                        + " déjà envoyé pour entretien #" + e.getIdEntretien() + " → ignoré");
                rappelsPlanifies.add(cle); // marquer pour ne plus vérifier
                return;
            }
        } catch (SQLException ex) {
            System.err.println("[NotifService] Erreur vérif BDD rappel : " + ex.getMessage());
        }

        long delayMs = java.time.Duration.between(LocalDateTime.now(), moment).toMillis();
        if (delayMs <= 0) return; // moment passé → ne pas planifier

        rappelsPlanifies.add(cle);

        int   idEntretien = e.getIdEntretien();
        int   idRecruteur = e.getIdRecruteur();

        scheduler.schedule(() -> {
            try {
                if (estToujoursPropose(idEntretien)) {
                    Notification notif = creerEtPersister(
                            idRecruteur, idEntretien, titre, message, type);
                    fireCallback(notif);
                    System.out.println("[NotifService] ✅ " + type.name()
                            + " envoyé pour entretien #" + idEntretien);
                }
            } catch (SQLException ex) {
                System.err.println("[NotifService] Erreur rappel #"
                        + idEntretien + " : " + ex.getMessage());
            } finally {
                rappelsPlanifies.remove(cle);
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        System.out.println("[NotifService] Rappel " + type.name()
                + " planifié pour entretien #" + idEntretien
                + " dans " + (delayMs / 60000) + " min");
    }

    // ════════════════════════════════════════════════════════════════
    //  DÉTECTION ET NOTIFICATION DES ANNULATIONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Détecte les entretiens annulés automatiquement aujourd'hui
     * et crée une notification pour chacun (sans doublon).
     */
    public void detecterEtNotifierAnnulations(int idRecruteur,
                                              Entretienservice entretienService) {
        scheduler.submit(() -> {
            try {
                List<EntretienAnnule> annules = getAnnulationsRecentes(idRecruteur);
                for (EntretienAnnule ea : annules) {
                    if (!notificationExisteEnBDD(ea.idEntretien, Notification.Type.ANNULATION)) {
                        String offreTitre = entretienService.getOffreTitre(ea.idOffre);
                        String titre   = "Entretien annulé automatiquement";
                        String message = "L'entretien prévu le " + ea.dateStr
                                + " pour le poste \"" + offreTitre
                                + "\" a été annulé car la date est dépassée.";
                        Notification notif = creerEtPersister(
                                idRecruteur, ea.idEntretien,
                                titre, message, Notification.Type.ANNULATION);
                        fireCallback(notif);
                        System.out.println("[NotifService] Annulation notifiée : entretien #"
                                + ea.idEntretien);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("[NotifService] Erreur annulations : " + ex.getMessage());
            }
        });
    }

    private static class EntretienAnnule {
        int    idEntretien;
        int    idOffre;
        String dateStr;
    }

    // ════════════════════════════════════════════════════════════════
    //  PERSISTANCE BDD  — toutes les méthodes SQL sont synchronized
    // ════════════════════════════════════════════════════════════════

    private synchronized Notification creerEtPersister(int idRecruteur, int idEntretien,
                                                       String titre, String message,
                                                       Notification.Type type) throws SQLException {
        Notification notif = new Notification(idRecruteur, idEntretien, titre, message, type);
        String sql = "INSERT INTO notification " +
                "(id_recruteur, id_entretien, titre, message, type) VALUES (?, ?, ?, ?, ?)";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt   (1, idRecruteur);
                ps.setInt   (2, idEntretien);
                ps.setString(3, titre);
                ps.setString(4, message);
                ps.setString(5, type.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) notif.setId(keys.getInt(1));
                }
            }
        }
        return notif;
    }

    public synchronized List<Notification> getNonLues(int idRecruteur) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE id_recruteur = ? AND lue = 0 " +
                "ORDER BY date_creation DESC";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, idRecruteur);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public synchronized List<Notification> getAll(int idRecruteur) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE id_recruteur = ? " +
                "ORDER BY date_creation DESC LIMIT 50";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, idRecruteur);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public synchronized void marquerLue(int idNotif) throws SQLException {
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE notification SET lue = 1 WHERE id = ?")) {
                ps.setInt(1, idNotif);
                ps.executeUpdate();
            }
        }
    }

    public synchronized void marquerToutesLues(int idRecruteur) throws SQLException {
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE notification SET lue = 1 WHERE id_recruteur = ? AND lue = 0")) {
                ps.setInt(1, idRecruteur);
                ps.executeUpdate();
            }
        }
    }

    public synchronized int countNonLues(int idRecruteur) throws SQLException {
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT COUNT(*) FROM notification WHERE id_recruteur = ? AND lue = 0")) {
                ps.setInt(1, idRecruteur);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // ── Requêtes internes ────────────────────────────────────────────

    private synchronized List<EntretienAnnule> getAnnulationsRecentes(int idRecruteur)
            throws SQLException {
        List<EntretienAnnule> liste = new ArrayList<>();
        String sql =
                "SELECT id_entretien, id_offre, date_entretien FROM entretien " +
                        "WHERE id_recruteur = ? " +
                        "  AND statut = 'annulé' " +
                        "  AND date_entretien < CURDATE() " +
                        "  AND DATE(date_creation) = CURDATE()";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, idRecruteur);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        EntretienAnnule ea = new EntretienAnnule();
                        ea.idEntretien = rs.getInt("id_entretien");
                        ea.idOffre     = rs.getInt("id_offre");
                        java.sql.Date d = rs.getDate("date_entretien");
                        ea.dateStr = d != null
                                ? d.toLocalDate().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : "date inconnue";
                        liste.add(ea);
                    }
                }
            }
        }
        return liste;
    }

    private synchronized boolean notificationExisteEnBDD(int idEntretien,
                                                         Notification.Type type)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification " +
                "WHERE id_entretien = ? AND type = ?";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, idEntretien);
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private synchronized boolean estToujoursPropose(int idEntretien) throws SQLException {
        String sql = "SELECT statut FROM entretien WHERE id_entretien = ?";
        synchronized (MyDatabase.getInstance()) {
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, idEntretien);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return "proposé".equals(rs.getString("statut"));
                }
            }
        }
        return false;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId          (rs.getInt      ("id"));
        n.setIdRecruteur (rs.getInt      ("id_recruteur"));
        n.setIdEntretien (rs.getInt      ("id_entretien"));
        n.setTitre       (rs.getString   ("titre"));
        n.setMessage     (rs.getString   ("message"));
        n.setType        (Notification.Type.valueOf(rs.getString("type")));
        n.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        n.setLue         (rs.getBoolean  ("lue"));
        return n;
    }

    private Connection conn() {
        return MyDatabase.getInstance().getConn();
    }

    private void fireCallback(Notification notif) {
        Consumer<Notification> cb = onNewNotification;
        if (cb != null) Platform.runLater(() -> cb.accept(notif));
    }

    // ── Messages ─────────────────────────────────────────────────────
    private String buildMessage24h(Entretien e) {
        String type = "présentiel".equals(e.getTypeEntretien()) ? "en présentiel" : "en visio";
        String heure = e.getHeureDebut().toLocalTime()
                .toString().substring(0, 5);
        return "Vous avez un entretien " + type + " prévu demain à "
                + heure + ". Pensez à vous préparer.";
    }

    private String buildMessage1h(Entretien e) {
        String type = "présentiel".equals(e.getTypeEntretien()) ? "en présentiel" : "en visio";
        String heure = e.getHeureDebut().toLocalTime()
                .toString().substring(0, 5);
        return "⚠️ Votre entretien " + type + " commence dans 1 heure (" + heure + "). Préparez-vous !";
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}