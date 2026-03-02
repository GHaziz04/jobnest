package tn.jobnest.gentretien.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GoogleCalendarReminderService {

    private static final String APPLICATION_NAME    = "JobNest Entretiens";
    private static final GsonFactory JSON_FACTORY   = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR          = "tokens";
    private static final String CREDENTIALS_PATH    = "/credentials.json";
    private static final List<String> SCOPES        =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // ─────────────────────────────────────────────────────────────
    //  AUTHENTIFICATION (réutilise les tokens existants)
    // ─────────────────────────────────────────────────────────────
    private static Credential getCredentials(NetHttpTransport transport) throws IOException {
        InputStream in = GoogleCalendarReminderService.class
                .getResourceAsStream(CREDENTIALS_PATH);
        if (in == null)
            throw new FileNotFoundException("credentials.json introuvable");

        GoogleClientSecrets secrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(
                        new FileDataStoreFactory(new File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static Calendar buildCalendarService()
            throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(transport, JSON_FACTORY, getCredentials(transport))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTHODE PRINCIPALE
    //  Crée (ou met à jour) un événement Calendar avec :
    //    - les participants (recruteur + candidats)
    //    - les rappels email 24h et 1h avant
    //
    //  Paramètres :
    //    titrePoste      : titre du poste / de l'offre
    //    typeEntretien   : "présentiel" ou "visio"
    //    lieuOuLien      : adresse ou lien Meet
    //    date            : LocalDate de l'entretien
    //    heureDebut      : LocalTime début
    //    heureFin        : LocalTime fin
    //    emailRecruteur  : email du recruteur
    //    emailsCandidat  : liste des emails des candidats
    //    googleEventId   : null pour créer, non-null pour modifier
    //
    //  Retourne l'ID de l'événement Google Calendar (à stocker en BDD)
    // ─────────────────────────────────────────────────────────────
    public static String creerOuMettreAJourEvenement(
            String titrePoste,
            String typeEntretien,
            String lieuOuLien,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            String emailRecruteur,
            List<String> emailsCandidat,
            String googleEventId     // null = création, sinon = mise à jour
    ) throws GeneralSecurityException, IOException {

        Calendar service = buildCalendarService();

        // ── Titre & description ───────────────────────────────────
        String titre = "Entretien JobNest — " + titrePoste;
        boolean isVisio = "visio".equalsIgnoreCase(typeEntretien);
        String description =
                "Entretien d'embauche organisé via JobNest\n\n" +
                        "Type : " + (isVisio ? "Visioconférence" : "Présentiel") + "\n" +
                        (isVisio
                                ? "Lien de connexion : " + (lieuOuLien != null ? lieuOuLien : "—")
                                : "Adresse : "           + (lieuOuLien != null ? lieuOuLien : "—")) + "\n\n" +
                        "⚠ Ce rappel est envoyé automatiquement par JobNest.\n" +
                        "Vous recevrez un rappel 24h avant et 1h avant l'entretien.";

        // ── Dates ISO 8601 ────────────────────────────────────────
        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String startStr = LocalDateTime.of(date, heureDebut).format(iso);
        String endStr   = LocalDateTime.of(date, heureFin)  .format(iso);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startStr))
                .setTimeZone("Africa/Tunis");
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endStr))
                .setTimeZone("Africa/Tunis");

        // ── Participants (attendees) ──────────────────────────────
        List<EventAttendee> attendees = new ArrayList<>();

        // Recruteur
        if (emailRecruteur != null && !emailRecruteur.isBlank()) {
            attendees.add(new EventAttendee()
                    .setEmail(emailRecruteur)
                    .setDisplayName("Recruteur")
                    .setResponseStatus("accepted"));
        }

        // Candidats
        for (String emailCand : emailsCandidat) {
            if (emailCand != null && !emailCand.isBlank()) {
                attendees.add(new EventAttendee()
                        .setEmail(emailCand)
                        .setDisplayName("Candidat")
                        .setResponseStatus("needsAction"));
            }
        }

        // ── Rappels email : 24h (1440 min) et 1h (60 min) ────────
        EventReminder rappel24h = new EventReminder()
                .setMethod("email")
                .setMinutes(24 * 60);   // 1440 minutes

        EventReminder rappel1h = new EventReminder()
                .setMethod("email")
                .setMinutes(60);

        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(rappel24h, rappel1h));

        // ── Lieu ──────────────────────────────────────────────────
        String lieu = !isVisio && lieuOuLien != null ? lieuOuLien : null;

        // ── Construire l'événement ────────────────────────────────
        Event event = new Event()
                .setSummary(titre)
                .setDescription(description)
                .setStart(start)
                .setEnd(end)
                .setAttendees(attendees)
                .setReminders(reminders);

        if (lieu != null) event.setLocation(lieu);

        // Ajouter le lien Meet si visio
        if (isVisio && lieuOuLien != null && lieuOuLien.startsWith("http")) {
            // On l'ajoute dans la description, pas en conferenceData
            // (conferenceData est déjà géré dans GoogleMeetService)
        }

        // ── Créer ou mettre à jour ────────────────────────────────
        Event result;
        if (googleEventId == null || googleEventId.isBlank()) {
            // CRÉATION
            result = service.events()
                    .insert("primary", event)
                    .setSendNotifications(true)   // ← envoie les emails d'invitation
                    .execute();
        } else {
            // MISE À JOUR
            result = service.events()
                    .update("primary", googleEventId, event)
                    .setSendNotifications(true)   // ← envoie les emails de modification
                    .execute();
        }

        System.out.println("[GoogleCalendar] Événement créé/mis à jour : " + result.getId());
        System.out.println("[GoogleCalendar] Lien : " + result.getHtmlLink());
        return result.getId();  // ID à stocker en BDD
    }

    // ─────────────────────────────────────────────────────────────
    //  SUPPRIMER un événement Calendar (quand l'entretien est supprimé)
    // ─────────────────────────────────────────────────────────────
    public static void supprimerEvenement(String googleEventId) {
        if (googleEventId == null || googleEventId.isBlank()) return;
        try {
            Calendar service = buildCalendarService();
            service.events()
                    .delete("primary", googleEventId)
                    .setSendNotifications(true)
                    .execute();
            System.out.println("[GoogleCalendar] Événement supprimé : " + googleEventId);
        } catch (Exception e) {
            System.err.println("[GoogleCalendar] Erreur suppression : " + e.getMessage());
        }
    }
}