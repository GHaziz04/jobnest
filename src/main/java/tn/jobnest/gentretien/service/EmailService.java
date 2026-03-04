package tn.jobnest.gentretien.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private final String username = "wassimchaieb2004@gmail.com";
    private final String password = "tehevhnbkelvttzw";

    // ─────────────────────────────────────────────────────────────
    //  ENVOI GÉNÉRIQUE (inchangé, pour compatibilité)
    // ─────────────────────────────────────────────────────────────

    public void sendEmail(String to, String subject, String messageText) {
        String htmlContent = buildTemplate(subject, messageText, "#1e3c72", "ℹ️");
        send(to, subject, htmlContent);
    }

    // ─────────────────────────────────────────────────────────────
    //  CAS 1 — Alerte : offre bientôt fermée, places non remplies
    // ─────────────────────────────────────────────────────────────

    public void sendAlerteBientotFermee(String to,
                                        String nomRecruteur,
                                        String titreOffre,
                                        String entreprise,
                                        String dateExpiration,
                                        long joursRestants,
                                        int nbCandidatures,
                                        int nbPostes) {

        String subject = "⚠️ Votre offre expire dans " + joursRestants
                + " jour(s) — " + titreOffre;

        String corps =
                "<p>Bonjour <strong>" + nomRecruteur + "</strong>,</p>" +

                        "<p>Votre offre d'emploi <strong>\"" + titreOffre + "\"</strong> " +
                        "expire dans <strong style='color:#d97706;'>" + joursRestants + " jour(s)</strong> " +
                        "et les places ne sont <strong>pas encore toutes pourvues</strong>.</p>" +

                        buildTableau(titreOffre, entreprise, dateExpiration,
                                joursRestants, nbCandidatures, nbPostes,
                                false) +

                        "<div style='background:#fff8e1; border-left:4px solid #f59e0b; " +
                        "padding:14px; border-radius:6px; margin-top:16px;'>" +
                        "<strong>💡 Actions recommandées :</strong><br><br>" +
                        "• <strong>Prolonger la date d'expiration</strong> pour laisser plus de temps aux candidats<br>" +
                        "• <strong>Booster la visibilité</strong> de l'offre pour attirer plus de profils<br>" +
                        "• <strong>Réviser les critères</strong> si trop peu de candidats correspondent" +
                        "</div>" +

                        "<p style='margin-top:20px; color:#6b7280; font-size:13px;'>" +
                        "Connectez-vous à JobNest pour gérer cette offre.</p>";

        String html = buildTemplate(subject, corps, "#f59e0b", "⚠️");
        send(to, subject, html);
    }

    // ─────────────────────────────────────────────────────────────
    //  CAS 2 — Alerte : places remplies, date encore loin
    // ─────────────────────────────────────────────────────────────

    public void sendAlerteRemplie(String to,
                                  String nomRecruteur,
                                  String titreOffre,
                                  String entreprise,
                                  String dateExpiration,
                                  long joursRestants,
                                  int nbCandidatures,
                                  int nbPostes) {

        String subject = "✅ Toutes les places sont pourvues — " + titreOffre;

        String corps =
                "<p>Bonjour <strong>" + nomRecruteur + "</strong>,</p>" +

                        "<p>Bonne nouvelle ! Toutes les places de votre offre " +
                        "<strong>\"" + titreOffre + "\"</strong> sont <strong>déjà pourvues</strong>, " +
                        "bien que la date d'expiration soit encore dans " +
                        "<strong>" + joursRestants + " jour(s)</strong>.</p>" +

                        buildTableau(titreOffre, entreprise, dateExpiration,
                                joursRestants, nbCandidatures, nbPostes,
                                true) +

                        "<div style='background:#ecfdf5; border-left:4px solid #10b981; " +
                        "padding:14px; border-radius:6px; margin-top:16px;'>" +
                        "<strong>💡 Actions recommandées :</strong><br><br>" +
                        "• <strong>Fermer l'offre dès maintenant</strong> pour ne plus recevoir " +
                        "de candidatures inutiles<br>" +
                        "• <strong>Commencer le processus de sélection</strong> parmi les candidatures reçues" +
                        "</div>" +

                        "<p style='margin-top:20px; color:#6b7280; font-size:13px;'>" +
                        "Connectez-vous à JobNest pour fermer ou gérer cette offre.</p>";

        String html = buildTemplate(subject, corps, "#10b981", "✅");
        send(to, subject, html);
    }

    // ─────────────────────────────────────────────────────────────
    //  TABLEAU RÉCAPITULATIF COMMUN
    // ─────────────────────────────────────────────────────────────

    private String buildTableau(String titreOffre, String entreprise,
                                String dateExpiration, long joursRestants,
                                int nbCandidatures, int nbPostes,
                                boolean placesRemplies) {

        String couleurPlaces = placesRemplies
                ? "background:#d1fae5; color:#065f46;"
                : "background:#fff3cd; color:#856404;";

        String texteJours = joursRestants <= 2
                ? "background:#fee2e2; color:#991b1b; font-weight:bold;"
                : "background:#f0fdf4; color:#166534;";

        return
                "<table style='border-collapse:collapse; width:100%; margin:18px 0; " +
                        "border-radius:8px; overflow:hidden;'>" +

                        ligne("📋 Offre",          titreOffre,   "#f8faff") +
                        ligne("🏢 Entreprise",     entreprise,   "#f0f4ff") +
                        ligne("📅 Date expiration", dateExpiration, "#f8faff") +

                        "<tr><td style='padding:10px 12px; background:#f0f4ff; " +
                        "font-weight:bold; width:40%; border-bottom:1px solid #e5e7eb;'>⏳ Jours restants</td>" +
                        "<td style='padding:10px 12px; " + texteJours +
                        " border-bottom:1px solid #e5e7eb;'>" + joursRestants + " jour(s)</td></tr>" +

                        "<tr><td style='padding:10px 12px; background:#f0f4ff; " +
                        "font-weight:bold; border-bottom:1px solid #e5e7eb;'>👥 Candidatures / Places</td>" +
                        "<td style='padding:10px 12px; " + couleurPlaces +
                        " font-weight:bold; border-bottom:1px solid #e5e7eb;'>"
                        + nbCandidatures + " / " + nbPostes
                        + (placesRemplies ? " ✅ Complet" : " (places disponibles)")
                        + "</td></tr>" +

                        "</table>";
    }

    private String ligne(String label, String valeur, String bg) {
        return "<tr>" +
                "<td style='padding:10px 12px; background:#f0f4ff; font-weight:bold; " +
                "width:40%; border-bottom:1px solid #e5e7eb;'>" + label + "</td>" +
                "<td style='padding:10px 12px; background:" + bg + "; " +
                "border-bottom:1px solid #e5e7eb;'>" + valeur + "</td>" +
                "</tr>";
    }

    // ─────────────────────────────────────────────────────────────
    //  ENVOI SMTP
    // ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlContent) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("[EmailService] ✅ Mail envoyé à : " + to
                    + " — Sujet : " + subject);
        } catch (MessagingException e) {
            System.err.println("[EmailService] ❌ Erreur envoi : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  TEMPLATE HTML GLOBAL
    // ─────────────────────────────────────────────────────────────

    private String buildTemplate(String title, String bodyHtml,
                                 String accentColor, String icon) {
        return
                "<div style='background:#f4f6f9; padding:40px; font-family:Arial, sans-serif;'>" +
                        "<div style='max-width:640px; margin:auto; background:white; border-radius:14px;" +
                        "overflow:hidden; box-shadow:0 5px 25px rgba(0,0,0,0.1);'>" +

                        // Header
                        "<div style='background:" + accentColor + "; color:white; " +
                        "text-align:center; padding:28px 32px;'>" +
                        "<div style='font-size:38px; margin-bottom:8px;'>" + icon + "</div>" +
                        "<h2 style='margin:0; font-size:20px; font-weight:bold;'>JobNest</h2>" +
                        "<p style='margin:6px 0 0 0; opacity:0.85; font-size:13px;'>Notification automatique</p>" +
                        "</div>" +

                        // Body
                        "<div style='padding:32px; color:#1f2937; font-size:14px; line-height:1.8;'>" +
                        bodyHtml +
                        "</div>" +

                        // Footer
                        "<div style='background:#f9fafb; text-align:center; padding:18px;" +
                        "font-size:12px; color:#9ca3af; border-top:1px solid #e5e7eb;'>" +
                        "© 2026 JobNest Platform — Ce message est envoyé automatiquement, ne pas répondre." +
                        "</div>" +

                        "</div></div>";
    }
}