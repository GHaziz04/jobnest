package tn.jobnest.gformation; // Assure-toi que le package correspond à ton projet

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestVersion {
    public static void main(String[] args) {
        // Ta clé API
        String API_KEY = "AIzaSyD6PrYY2FELKZ2jqexlg9OUnEqPlfeVWAM";

        // URL pour lister les modèles accessibles
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + API_KEY;


        try {
            System.out.println("🔍 Connexion à Google AI Studio en cours...");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Succès ! Voici les modèles que tu peux utiliser :");
                System.out.println(response.body());
            } else {
                System.out.println("❌ Erreur HTTP " + response.statusCode());
                System.out.println(response.body());
            }

        } catch (Exception e) {
            System.err.println("💥 Erreur lors du test : " + e.getMessage());
            e.printStackTrace();
        }
    }
}