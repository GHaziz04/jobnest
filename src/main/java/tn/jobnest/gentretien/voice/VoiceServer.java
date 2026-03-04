package tn.jobnest.gentretien.voice;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class VoiceServer {

    private static HttpServer server;

    public static void start() {
        try {
            if (server != null) return;

            server = HttpServer.create(new InetSocketAddress(5555), 0);

            // ================= SERVE HTML =================
            server.createContext("/voice.html", exchange -> {
                addCorsHeaders(exchange);

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                InputStream is = VoiceServer.class
                        .getClassLoader()
                        .getResourceAsStream("voice.html");

                if (is == null) {
                    String resp = "voice.html NOT FOUND";
                    byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                    return;
                }

                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            // ================= RECEIVE VOICE TEXT =================
            server.createContext("/voice", exchange -> {
                // ✅ CORS headers — indispensable pour que le navigateur accepte la requête
                addCorsHeaders(exchange);

                // Preflight OPTIONS (navigateur envoie ça avant POST)
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String text = sb.toString().trim();

                System.out.println("[VoiceServer] Reçu : " + text);
                VoiceBridge.dispatch(text);

                byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            server.start();
            System.out.println("[VoiceServer] ✅ Démarré → http://localhost:5555/voice.html");

        } catch (Exception e) {
            System.err.println("[VoiceServer] ❌ Erreur démarrage : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("[VoiceServer] ✅ Arrêté.");
        }
    }

    // ✅ Headers CORS — permet au navigateur (localhost:5555) d'envoyer à Java
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}