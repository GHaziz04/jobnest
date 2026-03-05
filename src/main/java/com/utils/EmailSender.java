package com.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.List;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "moatazmansour391@gmail.com";
    private static final String PASSWORD   = "ydbb dfor iaoe sqgv"; // App Password Gmail

    // ================================================================
    //  MÉTHODE EXISTANTE — NE PAS MODIFIER
    // ================================================================

    /**
     * Envoie un email avec le code OTP
     */
    public static void sendOTP(String toEmail, String otp) throws Exception {
        System.out.println("📧 Tentative d'envoi d'email à : " + toEmail);
        try {
            Session session = buildSession();
            session.setDebug(true);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Code OTP - Réinitialisation de mot de passe");

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px; }
                        .container { max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1); }
                        .header { background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center; }
                        .header h1 { color:white;margin:0;font-size:28px; }
                        .content { padding:40px 30px; }
                        .otp-box { background-color:#f9fafb;border:2px dashed #d4966d;border-radius:8px;padding:25px;text-align:center;margin:30px 0; }
                        .otp-code { font-size:40px;font-weight:bold;color:#d4966d;letter-spacing:8px;font-family:'Courier New',monospace; }
                        .info { color:#6b7280;font-size:15px;line-height:1.6; }
                        .warning { background-color:#fef3c7;border-left:4px solid #f59e0b;padding:15px;margin:20px 0;border-radius:4px;color:#92400e; }
                        .footer { background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb; }
                        .logo { font-size:32px;margin-bottom:10px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">🏢</div>
                            <h1>JobNest</h1>
                            <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
                        </div>
                        <div class="content">
                            <h2 style="color:#1f2937;margin-top:0;">Réinitialisation de mot de passe</h2>
                            <p class="info">
                                Vous avez demandé à réinitialiser votre mot de passe JobNest.
                                Utilisez le code ci-dessous pour continuer :
                            </p>
                            <div class="otp-box">
                                <div style="color:#9ca3af;font-size:12px;margin-bottom:15px;text-transform:uppercase;letter-spacing:2px;">
                                    Votre code de vérification
                                </div>
                                <div class="otp-code">%s</div>
                            </div>
                            <div class="warning">
                                <strong>⏰ Important :</strong> Ce code expire dans <strong>5 minutes</strong>.
                            </div>
                            <p class="info">
                                Si vous n'avez pas demandé cette réinitialisation,
                                veuillez ignorer cet email. Votre mot de passe restera inchangé.
                            </p>
                            <p class="info" style="margin-top:30px;">
                                Besoin d'aide ? Contactez notre support.
                            </p>
                        </div>
                        <div class="footer">
                            <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                            <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, otp);

            message.setContent(htmlContent, "text/html; charset=utf-8");
            System.out.println("📤 Envoi en cours...");
            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à : " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ ERREUR MessagingException:");
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Authentication failed"))
                    throw new Exception("Authentification échouée. Vérifiez votre App Password Gmail.");
                else if (errorMsg.contains("connect"))
                    throw new Exception("Impossible de se connecter au serveur SMTP.");
                else if (errorMsg.contains("Invalid Addresses"))
                    throw new Exception("Adresse email invalide : " + toEmail);
                else
                    throw new Exception("Erreur d'envoi d'email : " + errorMsg);
            }
            throw new Exception("Erreur lors de l'envoi de l'email.");
        } catch (Exception e) {
            System.err.println("❌ ERREUR Exception:");
            e.printStackTrace();
            throw new Exception("Erreur inattendue : " + e.getMessage());
        }
    }

    // ================================================================
    //  NOUVEAU — Réinitialisation mot de passe (même style que OTP)
    // ================================================================

    /**
     * Envoie un email de réinitialisation avec un mot de passe temporaire.
     * Même charte graphique que sendOTP.
     *
     * @param toEmail      Email du destinataire
     * @param prenom       Prénom affiché dans l'email
     * @param tempPassword Mot de passe temporaire généré
     */
    public static void sendPasswordReset(String toEmail, String prenom, String tempPassword) throws Exception {
        System.out.println("🔑 Envoi réinitialisation MDP à : " + toEmail);

        Session session = buildSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("🔑 Nouveau mot de passe temporaire - JobNest");

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px; }
                    .container { max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1); }
                    .header { background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center; }
                    .header h1 { color:white;margin:0;font-size:28px; }
                    .content { padding:40px 30px; }
                    .pwd-box { background-color:#f9fafb;border:2px dashed #d4966d;border-radius:8px;padding:25px;text-align:center;margin:30px 0; }
                    .pwd-code { font-size:28px;font-weight:bold;color:#d4966d;letter-spacing:4px;font-family:'Courier New',monospace; }
                    .info { color:#6b7280;font-size:15px;line-height:1.6; }
                    .warning { background-color:#fef3c7;border-left:4px solid #f59e0b;padding:15px;margin:20px 0;border-radius:4px;color:#92400e; }
                    .btn { display:inline-block;background:linear-gradient(135deg,#2f3655,#3a4268);color:white;padding:12px 30px;border-radius:6px;text-decoration:none;font-weight:bold;margin-top:10px; }
                    .footer { background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb; }
                    .logo { font-size:32px;margin-bottom:10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🏢</div>
                        <h1>JobNest</h1>
                        <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
                    </div>
                    <div class="content">
                        <h2 style="color:#1f2937;margin-top:0;">Bonjour %s 👋</h2>
                        <p class="info">
                            Votre mot de passe a été réinitialisé par l'administrateur JobNest.
                            Voici votre mot de passe temporaire :
                        </p>
                        <div class="pwd-box">
                            <div style="color:#9ca3af;font-size:12px;margin-bottom:15px;text-transform:uppercase;letter-spacing:2px;">
                                Mot de passe temporaire
                            </div>
                            <div class="pwd-code">%s</div>
                        </div>
                        <div class="warning">
                            <strong>⚠️ Important :</strong> Connectez-vous et <strong>changez ce mot de passe immédiatement</strong>
                            depuis votre espace personnel.
                        </div>
                        <p class="info">
                            Si vous n'avez pas demandé cette réinitialisation,
                            contactez notre support immédiatement.
                        </p>
                        <div style="text-align:center;margin-top:30px;">
                            <a href="https://jobnest.com/login" class="btn">Se connecter →</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                        <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                    </div>
                </div>
            </body>
            </html>
            """, prenom, tempPassword);

        message.setContent(html, "text/html; charset=utf-8");
        Transport.send(message);
        System.out.println("✅ Email réinitialisation MDP envoyé à : " + toEmail);
    }

    // ================================================================
    //  NOUVEAU — Email en masse (annonce / newsletter)
    // ================================================================

    /**
     * Interface callback pour suivre la progression de l'envoi.
     */
    public interface BulkProgressCallback {
        /** Appelé après chaque envoi. current = index actuel (1-based), total = nombre total */
        void onProgress(int current, int total, String email, boolean success);
    }

    /**
     * Représente un destinataire pour l'envoi en masse.
     */
    public static class Recipient {
        public final String email;
        public final String prenom;
        public Recipient(String email, String prenom) {
            this.email  = email;
            this.prenom = prenom != null ? prenom : "";
        }
    }

    /**
     * Envoie un email d'annonce à une liste de destinataires.
     * Même charte graphique que sendOTP.
     * Un seul Transport ouvert pour tous les envois (performant).
     *
     * @param recipients Liste des destinataires
     * @param subject    Objet de l'email
     * @param bodyText   Corps du message (texte brut, sera mis en forme automatiquement)
     * @param callback   Optionnel — appelé après chaque envoi pour mettre à jour la progression
     * @return Nombre d'envois réussis
     */
    public static int sendBulkAnnouncement(
            List<Recipient> recipients,
            String subject,
            String bodyText,
            BulkProgressCallback callback) {

        System.out.println("📨 Démarrage envoi en masse : " + recipients.size() + " destinataires");
        int success = 0;

        Session session = buildSession();

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect("smtp.gmail.com", 465, FROM_EMAIL, PASSWORD);

            for (int i = 0; i < recipients.size(); i++) {
                Recipient r = recipients.get(i);
                boolean ok  = false;
                try {
                    Message msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
                    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(r.email));
                    msg.setSubject(subject);
                    msg.setContent(buildAnnouncementHtml(r.prenom, subject, bodyText),
                            "text/html; charset=utf-8");
                    transport.sendMessage(msg, msg.getAllRecipients());
                    success++;
                    ok = true;
                    System.out.println("✅ [" + (i+1) + "/" + recipients.size() + "] " + r.email);
                } catch (Exception e) {
                    System.err.println("❌ [" + (i+1) + "/" + recipients.size() + "] " + r.email + " — " + e.getMessage());
                }
                if (callback != null) callback.onProgress(i + 1, recipients.size(), r.email, ok);
            }

        } catch (Exception e) {
            System.err.println("❌ Impossible d'ouvrir le transport SMTP : " + e.getMessage());
        }

        System.out.println("📊 Résultat : " + success + "/" + recipients.size() + " envoyés");
        return success;
    }




    /**
     * Envoie un email de vérification de compte avec un token unique.
     */
    public static void sendVerificationEmail(String toEmail, String prenom, String token) throws Exception {

        // ── Port dynamique récupéré depuis le serveur ──
        int    port            = VerificationServer.getPort();
        String verificationUrl = "http://localhost:" + port + "/verify?token=" + token;


        Session session = buildSession();
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("✅ Vérifiez votre compte JobNest");

        String html = String.format("""
        <!DOCTYPE html><html><head>
        <style>
            body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px;}
            .container{max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1);}
            .header{background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center;}
            .header h1{color:white;margin:0;font-size:28px;}
            .content{padding:40px 30px;}
            .verify-box{background-color:#f0fdf4;border:2px solid #86efac;border-radius:8px;padding:20px 25px;margin:25px 0;text-align:center;}
            .info{color:#6b7280;font-size:15px;line-height:1.6;}
            .btn{display:inline-block;background:linear-gradient(135deg,#2f3655,#3a4268);color:white;padding:14px 35px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;}
            .token-box{background:#f9fafb;border:1px dashed #d4966d;border-radius:6px;padding:12px;margin:15px 0;font-family:'Courier New',monospace;font-size:14px;color:#374151;word-break:break-all;}
            .footer{background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb;}
        </style>
        </head><body>
        <div class="container">
            <div class="header">
                <div style="font-size:32px;margin-bottom:10px;">🏢</div>
                <h1>JobNest</h1>
                <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
            </div>
            <div class="content">
                <div class="verify-box">
                    <div style="font-size:48px;margin-bottom:10px;">📧</div>
                    <h2 style="color:#16a34a;margin:0;">Vérifiez votre email</h2>
                </div>
                <p class="info">Bonjour <strong>%s</strong>,</p>
                <p class="info">
                    Merci de vous être inscrit sur <strong>JobNest</strong> ! 🎉<br>
                    Pour activer votre compte, cliquez sur le bouton ci-dessous :
                </p>
                <div style="text-align:center;margin:30px 0;">
                    <a href="%s" class="btn">✅ Vérifier mon compte</a>
                </div>
                <p class="info" style="font-size:13px;">
                    Ou copiez ce lien dans votre navigateur :<br>
                    <div class="token-box">%s</div>
                </p>
                <p class="info" style="color:#ef4444;font-size:13px;">
                    ⏰ Ce lien expire dans <strong>24 heures</strong>.
                </p>
                <p class="info" style="font-size:13px;color:#9ca3af;">
                    Si vous n'avez pas créé de compte JobNest, ignorez cet email.
                </p>
            </div>
            <div class="footer">
                <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
            </div>
        </div>
        </body></html>
        """, prenom, verificationUrl, verificationUrl);

        message.setContent(html, "text/html; charset=utf-8");
        Transport.send(message);
        System.out.println("✅ Email de vérification envoyé à : " + toEmail);
    }

    // ================================================================
    //  NOUVEAU — Notification compte bloqué / réactivé
    // ================================================================

    /**
     * Notifie l'utilisateur que son compte a été bloqué par un admin.
     */
    public static void sendBlockedNotification(String toEmail, String prenom) throws Exception {
        Session session = buildSession();
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("⚠️ Votre compte JobNest a été suspendu");
        msg.setContent(String.format("""
            <!DOCTYPE html><html><head>
            <style>
                body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px;}
                .container{max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1);}
                .header{background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center;}
                .header h1{color:white;margin:0;font-size:28px;}
                .content{padding:40px 30px;}
                .alert-box{background-color:#fef2f2;border:2px solid #fca5a5;border-radius:8px;padding:20px 25px;margin:25px 0;text-align:center;}
                .alert-icon{font-size:48px;margin-bottom:10px;}
                .info{color:#6b7280;font-size:15px;line-height:1.6;}
                .btn{display:inline-block;background:linear-gradient(135deg,#2f3655,#3a4268);color:white;padding:12px 30px;border-radius:6px;text-decoration:none;font-weight:bold;}
                .footer{background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb;}
            </style>
            </head><body>
            <div class="container">
                <div class="header">
                    <div style="font-size:32px;margin-bottom:10px;">🏢</div>
                    <h1>JobNest</h1>
                    <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
                </div>
                <div class="content">
                    <div class="alert-box">
                        <div class="alert-icon">🔒</div>
                        <h2 style="color:#dc2626;margin:0;">Compte suspendu</h2>
                    </div>
                    <p class="info">Bonjour <strong>%s</strong>,</p>
                    <p class="info">
                        Votre compte JobNest a été <strong>suspendu</strong> par un administrateur.
                        Vous ne pouvez plus vous connecter à la plateforme.
                    </p>
                    <p class="info">
                        Si vous pensez qu'il s'agit d'une erreur ou souhaitez contester cette décision,
                        veuillez contacter notre équipe support :
                    </p>
                    <div style="text-align:center;margin-top:25px;">
                        <a href="mailto:support@jobnest.com" class="btn">Contacter le support</a>
                    </div>
                </div>
                <div class="footer">
                    <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                    <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                </div>
            </div>
            </body></html>
            """, prenom), "text/html; charset=utf-8");
        Transport.send(msg);
        System.out.println("✅ Notification blocage envoyée à : " + toEmail);
    }

    /**
     * Notifie l'utilisateur que son compte a été réactivé.
     */
    public static void sendReactivatedNotification(String toEmail, String prenom) throws Exception {
        Session session = buildSession();
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("✅ Votre compte JobNest a été réactivé");
        msg.setContent(String.format("""
            <!DOCTYPE html><html><head>
            <style>
                body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px;}
                .container{max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1);}
                .header{background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center;}
                .header h1{color:white;margin:0;font-size:28px;}
                .content{padding:40px 30px;}
                .success-box{background-color:#f0fdf4;border:2px solid #86efac;border-radius:8px;padding:20px 25px;margin:25px 0;text-align:center;}
                .info{color:#6b7280;font-size:15px;line-height:1.6;}
                .btn{display:inline-block;background:linear-gradient(135deg,#2f3655,#3a4268);color:white;padding:12px 30px;border-radius:6px;text-decoration:none;font-weight:bold;}
                .footer{background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb;}
            </style>
            </head><body>
            <div class="container">
                <div class="header">
                    <div style="font-size:32px;margin-bottom:10px;">🏢</div>
                    <h1>JobNest</h1>
                    <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
                </div>
                <div class="content">
                    <div class="success-box">
                        <div style="font-size:48px;margin-bottom:10px;">✅</div>
                        <h2 style="color:#16a34a;margin:0;">Compte réactivé</h2>
                    </div>
                    <p class="info">Bonjour <strong>%s</strong>,</p>
                    <p class="info">
                        Bonne nouvelle ! Votre compte JobNest a été <strong>réactivé</strong>.
                        Vous pouvez désormais vous connecter normalement à la plateforme.
                    </p>
                    <div style="text-align:center;margin-top:25px;">
                        <a href="https://jobnest.com/login" class="btn">Se connecter →</a>
                    </div>
                </div>
                <div class="footer">
                    <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                    <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                </div>
            </div>
            </body></html>
            """, prenom), "text/html; charset=utf-8");
        Transport.send(msg);
        System.out.println("✅ Notification réactivation envoyée à : " + toEmail);
    }

    // ─── HTML de l'annonce ──────────────────────────────────────────
    private static String buildAnnouncementHtml(String prenom, String subject, String bodyText) {
        // Convertir les sauts de ligne en <br> pour l'affichage HTML
        String bodyHtml = bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f5f7;margin:0;padding:20px; }
                    .container { max-width:600px;margin:0 auto;background-color:white;border-radius:10px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1); }
                    .header { background:linear-gradient(135deg,#2f3655 0%%,#3a4268 100%%);padding:30px;text-align:center; }
                    .header h1 { color:white;margin:0;font-size:28px; }
                    .content { padding:40px 30px; }
                    .msg-box { background-color:#f9fafb;border-left:4px solid #d4966d;border-radius:0 8px 8px 0;padding:20px 25px;margin:25px 0;color:#374151;font-size:15px;line-height:1.7; }
                    .info { color:#6b7280;font-size:15px;line-height:1.6; }
                    .footer { background-color:#f9fafb;padding:20px;text-align:center;color:#9ca3af;font-size:12px;border-top:1px solid #e5e7eb; }
                    .logo { font-size:32px;margin-bottom:10px; }
                    .divider { border:none;border-top:1px solid #e5e7eb;margin:25px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🏢</div>
                        <h1>JobNest</h1>
                        <p style="color:#d0d3ff;margin:5px 0 0 0;">Plateforme de recrutement</p>
                    </div>
                    <div class="content">
                        <h2 style="color:#1f2937;margin-top:0;">%s</h2>
                        <p class="info">Bonjour <strong>%s</strong>,</p>
                        <div class="msg-box">%s</div>
                        <hr class="divider">
                        <p class="info" style="font-size:13px;color:#9ca3af;">
                            Vous recevez cet email car vous êtes inscrit sur JobNest.<br>
                            Pour toute question, contactez notre support.
                        </p>
                    </div>
                    <div class="footer">
                        <p style="margin:5px 0;">© 2026 JobNest - Tous droits réservés</p>
                        <p style="margin:5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                    </div>
                </div>
            </body>
            </html>
            """, subject, prenom.isEmpty() ? "cher utilisateur" : prenom, bodyHtml);
    }

    // ================================================================
    //  UTILITAIRES
    // ================================================================

    /** Construit la Session SMTP Gmail (SSL port 465) */
    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.ssl.enable",      "true");
        props.put("mail.smtp.port",            "465");
        props.put("mail.smtp.ssl.protocols",   "TLSv1.2");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
        props.put("mail.smtp.timeout",         "10000");
        props.put("mail.smtp.connectiontimeout","10000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    public static void testConfiguration() {
        System.out.println("🔍 Test de configuration email...");
        System.out.println("FROM_EMAIL: " + FROM_EMAIL);
        System.out.println("PASSWORD configuré: " + (!PASSWORD.isEmpty() ? "✓" : "✗"));
        try {
            System.out.println("\n📧 Envoi d'un email de test...");
            sendOTP(FROM_EMAIL, "123456");
            System.out.println("\n✅ Configuration valide !");
        } catch (Exception e) {
            System.err.println("\n❌ Configuration invalide : " + e.getMessage());
        }
    }

    public static void main(String[] args) { testConfiguration(); }
}