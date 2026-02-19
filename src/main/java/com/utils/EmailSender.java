package com.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "moatazmansour391@gmail.com";
    private static final String PASSWORD = "ydbb dfor iaoe sqgv"; // App Password Gmail

    /**
     * Envoie un email avec le code OTP
     * @param toEmail Email du destinataire
     * @param otp Code OTP à envoyer
     * @throws Exception Si l'envoi échoue
     */
    public static void sendOTP(String toEmail, String otp) throws Exception {

        System.out.println("📧 Tentative d'envoi d'email à : " + toEmail);

        try {
            // Configuration SMTP pour Gmail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            // Timeout configuration (pour éviter les blocages)
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            // Créer la session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                }
            });

            // Activer le debug (pour voir les détails dans la console)
            session.setDebug(true);

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "JobNest"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail)
            );

            message.setSubject("🔐 Code OTP - Réinitialisation de mot de passe");

            // Message HTML professionnel
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: 'Segoe UI', Arial, sans-serif;
                            background-color: #f4f5f7;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: white;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #2f3655 0%%, #3a4268 100%%);
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            color: white;
                            margin: 0;
                            font-size: 28px;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .otp-box {
                            background-color: #f9fafb;
                            border: 2px dashed #d4966d;
                            border-radius: 8px;
                            padding: 25px;
                            text-align: center;
                            margin: 30px 0;
                        }
                        .otp-code {
                            font-size: 40px;
                            font-weight: bold;
                            color: #d4966d;
                            letter-spacing: 8px;
                            font-family: 'Courier New', monospace;
                        }
                        .info {
                            color: #6b7280;
                            font-size: 15px;
                            line-height: 1.6;
                        }
                        .warning {
                            background-color: #fef3c7;
                            border-left: 4px solid #f59e0b;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                            color: #92400e;
                        }
                        .footer {
                            background-color: #f9fafb;
                            padding: 20px;
                            text-align: center;
                            color: #9ca3af;
                            font-size: 12px;
                            border-top: 1px solid #e5e7eb;
                        }
                        .logo {
                            font-size: 32px;
                            margin-bottom: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">🏢</div>
                            <h1>JobNest</h1>
                            <p style="color: #d0d3ff; margin: 5px 0 0 0;">Plateforme de recrutement</p>
                        </div>
                        <div class="content">
                            <h2 style="color: #1f2937; margin-top: 0;">Réinitialisation de mot de passe</h2>
                            <p class="info">
                                Vous avez demandé à réinitialiser votre mot de passe JobNest. 
                                Utilisez le code ci-dessous pour continuer :
                            </p>
                            
                            <div class="otp-box">
                                <div style="color: #9ca3af; font-size: 12px; margin-bottom: 15px; text-transform: uppercase; letter-spacing: 2px;">
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
                            
                            <p class="info" style="margin-top: 30px;">
                                Besoin d'aide ? Contactez notre support.
                            </p>
                        </div>
                        <div class="footer">
                            <p style="margin: 5px 0;">© 2026 JobNest - Tous droits réservés</p>
                            <p style="margin: 5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, otp);

            // Définir le contenu HTML
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Envoyer le message
            System.out.println("📤 Envoi en cours...");
            Transport.send(message);

            System.out.println("✅ Email envoyé avec succès à : " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ ERREUR MessagingException:");
            e.printStackTrace();

            // Messages d'erreur plus détaillés
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Authentication failed")) {
                    throw new Exception("Authentification échouée. Vérifiez votre App Password Gmail.");
                } else if (errorMsg.contains("connect")) {
                    throw new Exception("Impossible de se connecter au serveur SMTP. Vérifiez votre connexion Internet.");
                } else if (errorMsg.contains("Invalid Addresses")) {
                    throw new Exception("Adresse email invalide : " + toEmail);
                } else {
                    throw new Exception("Erreur d'envoi d'email : " + errorMsg);
                }
            }
            throw new Exception("Erreur lors de l'envoi de l'email. Vérifiez votre connexion.");

        } catch (Exception e) {
            System.err.println("❌ ERREUR Exception:");
            e.printStackTrace();
            throw new Exception("Erreur inattendue : " + e.getMessage());
        }
    }

    /**
     * Méthode de test pour vérifier la configuration
     */
    public static void testConfiguration() {
        System.out.println("🔍 Test de configuration email...");
        System.out.println("FROM_EMAIL: " + FROM_EMAIL);
        System.out.println("PASSWORD configuré: " + (PASSWORD != null && !PASSWORD.isEmpty() ? "✓" : "✗"));

        try {
            System.out.println("\n📧 Envoi d'un email de test...");
            sendOTP(FROM_EMAIL, "123456");
            System.out.println("\n✅ Configuration valide! Email de test envoyé.");
        } catch (Exception e) {
            System.err.println("\n❌ Configuration invalide:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Méthode principale pour tester
     */
    public static void main(String[] args) {
        testConfiguration();
    }
}
