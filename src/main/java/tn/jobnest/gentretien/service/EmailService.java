package tn.jobnest.gentretien.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private final String username = "wassimchaieb2004@gmail.com";
    private final String password = "tehevhnbkelvttzw";

    public void sendEmail(String to, String subject, String messageText) {

        String htmlContent = buildTemplate(subject, messageText);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );

            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Mail envoyé avec succès");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private String buildTemplate(String title, String content) {


        return "<div style='background:#f4f6f9;padding:40px;font-family:Arial'>"
                + "<div style='max-width:600px;margin:auto;background:white;"
                + "border-radius:12px;overflow:hidden;"
                + "box-shadow:0 5px 20px rgba(0,0,0,0.1);'>"

                + "<div style='background:#1e3c72;color:white;text-align:center;padding:25px'>"
                + "<h2 style='margin:10px 0 0 0;'>JobNest</h2>"
                + "</div>"

                + "<div style='padding:30px;color:#333'>"
                + "<h3 style='color:#1e3c72;'>" + title + "</h3>"

                + "<p style='line-height:1.7;font-size:15px;'>"
                + content.replace("\n", "<br>")
                + "</p>"

                + "<div style='text-align:center;margin-top:25px;'>"
                + "<a href='http://localhost:8080' "
                + "style='display:inline-block;padding:12px 22px;"
                + "background:#1e3c72;color:white;text-decoration:none;"
                + "border-radius:8px;font-weight:bold;'>"
                + "Voir l'offre"
                + "</a>"
                + "</div>"

                + "</div>"

                + "<div style='background:#f0f0f0;text-align:center;"
                + "padding:15px;font-size:12px;color:#777'>"
                + "© 2026 JobNest Platform"
                + "</div>"

                + "</div>"
                + "</div>";
    }
}