package tn.jobnest.gformation.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import tn.jobnest.gformation.model.QuizQuestion;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServiceGemini {

    // On garde ton URL et ton modèle d'origine
    private static final String API_KEY = "AIzaSyD6PrYY2FELKZ2jqexlg9OUnEqPlfeVWAM";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + API_KEY;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ServiceGemini() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // Le nom doit être genererQuiz pour ton QuizController
    public List<QuizQuestion> genererQuiz(String contenuCours) throws IOException {
        String prompt = "Génère un quiz de 3 questions QCM basées sur le texte suivant. " +
                "Réponds uniquement au format JSON pur. " +
                "Format : [{\"question\": \"...\", \"options\": [\"opt1\", \"opt2\", \"opt3\"], \"reponseCorrecte\": \"opt1\"}] " +
                "Texte : " + contenuCours;

        // Échappement sécurisé du prompt pour le JSON
        String jsonPayload = "{\"contents\": [{\"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", " ") + "\"}]}]}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Erreur API : " + response.code());

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);

            // Extraction du texte JSON de la réponse Gemini
            String rawJson = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Nettoyage des balises Markdown
            rawJson = rawJson.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(rawJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuizQuestion.class));
        }
    }
}