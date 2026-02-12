package tn.jobnest.gentretien.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GoogleMeetService {

    private static final String APPLICATION_NAME = "JobNest Entretiens";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Crée un objet Credential autorisé.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Charger les secrets du client depuis credentials.json
        InputStream in = GoogleMeetService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Fichier credentials.json introuvable dans resources");
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Construire le flux d'autorisation
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Crée un événement Google Meet et retourne le lien de la réunion.
     *
     * @param titre Titre de l'entretien
     * @param description Description de l'entretien
     * @param dateDebut Date et heure de début au format ISO 8601
     * @param dateFin Date et heure de fin au format ISO 8601
     * @return Le lien Google Meet
     */
    public static String creerMeetingLink(String titre, String description, String dateDebut, String dateFin)
            throws GeneralSecurityException, IOException {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Créer l'événement
        Event event = new Event()
                .setSummary(titre)
                .setDescription(description);

        // Définir la date et l'heure de début
        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(dateDebut))
                .setTimeZone("Africa/Tunis");
        event.setStart(start);

        // Définir la date et l'heure de fin
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(dateFin))
                .setTimeZone("Africa/Tunis");
        event.setEnd(end);

        // Ajouter la configuration Google Meet
        ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey();
        conferenceSolutionKey.setType("hangoutsMeet");

        CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest();
        createConferenceRequest.setRequestId(UUID.randomUUID().toString());
        createConferenceRequest.setConferenceSolutionKey(conferenceSolutionKey);

        ConferenceData conferenceData = new ConferenceData();
        conferenceData.setCreateRequest(createConferenceRequest);

        event.setConferenceData(conferenceData);

        // Insérer l'événement dans le calendrier
        event = service.events().insert("primary", event)
                .setConferenceDataVersion(1)
                .execute();

        // Retourner le lien Meet
        String meetLink = event.getHangoutLink();
        if (meetLink == null && event.getConferenceData() != null) {
            meetLink = event.getConferenceData().getEntryPoints().get(0).getUri();
        }

        return meetLink;
    }
}