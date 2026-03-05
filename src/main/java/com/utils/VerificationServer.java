package com.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class VerificationServer {

    private static HttpServer server;
    private static int        port = 8080; // port par défaut

    /**
     * Trouve un port libre à partir de 8080.
     */
    private static int findAvailablePort() {
        for (int p = 8080; p <= 8090; p++) {
            try (ServerSocket ss = new ServerSocket(p)) {
                ss.setReuseAddress(true);
                return p;
            } catch (Exception ignored) {}
        }
        // Si tous les ports 8080-8090 sont pris, laisser l'OS choisir
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        } catch (Exception e) {
            return 8080;
        }
    }

    /**
     * Démarre le serveur HTTP sur le premier port disponible.
     */
    public static void start() {
        try {
            port   = findAvailablePort();
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/verify", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String token = null;

                if (query != null && query.startsWith("token=")) {
                    token = query.substring(6);
                }

                String responseHtml;
                int    statusCode;

                if (token != null && AccountVerifier.verifyAccount(token)) {
                    statusCode   = 200;
                    responseHtml = """
                        <!DOCTYPE html>
                        <html lang="fr">
                        <head>
                            <meta charset="UTF-8">
                            <title>JobNest - Compte activé</title>
                            <style>
                                * { margin:0; padding:0; box-sizing:border-box; }
                                body {
                                    font-family: 'Segoe UI', Arial, sans-serif;
                                    background: linear-gradient(135deg, #2f3655, #3a4268);
                                    min-height: 100vh;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                }
                                .card {
                                    background: white;
                                    border-radius: 16px;
                                    padding: 50px 40px;
                                    text-align: center;
                                    max-width: 480px;
                                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                }
                                .icon { font-size: 72px; margin-bottom: 20px; }
                                h1 { color: #16a34a; font-size: 28px; margin-bottom: 12px; }
                                p  { color: #6b7280; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                                .badge {
                                    display: inline-block;
                                    background: #f0fdf4;
                                    border: 2px solid #86efac;
                                    color: #16a34a;
                                    padding: 10px 25px;
                                    border-radius: 50px;
                                    font-weight: bold;
                                    font-size: 15px;
                                    margin-bottom: 30px;
                                }
                                .info {
                                    background: #f9fafb;
                                    border-radius: 10px;
                                    padding: 15px 20px;
                                    color: #374151;
                                    font-size: 14px;
                                    border-left: 4px solid #d4966d;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <div class="icon">✅</div>
                                <h1>Compte activé !</h1>
                                <p>Votre compte <strong>JobNest</strong> a été vérifié et activé avec succès.</p>
                                <div class="badge">🎉 Vous êtes prêt à vous connecter</div>
                                <div class="info">
                                    Retournez sur l'application JobNest<br>
                                    et connectez-vous avec vos identifiants.
                                </div>
                            </div>
                        </body>
                        </html>
                        """;
                } else {
                    statusCode   = 400;
                    responseHtml = """
                        <!DOCTYPE html>
                        <html lang="fr">
                        <head>
                            <meta charset="UTF-8">
                            <title>JobNest - Lien invalide</title>
                            <style>
                                * { margin:0; padding:0; box-sizing:border-box; }
                                body {
                                    font-family: 'Segoe UI', Arial, sans-serif;
                                    background: linear-gradient(135deg, #2f3655, #3a4268);
                                    min-height: 100vh;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                }
                                .card {
                                    background: white;
                                    border-radius: 16px;
                                    padding: 50px 40px;
                                    text-align: center;
                                    max-width: 480px;
                                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                                }
                                .icon { font-size: 72px; margin-bottom: 20px; }
                                h1 { color: #dc2626; font-size: 28px; margin-bottom: 12px; }
                                p  { color: #6b7280; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                                .badge {
                                    display: inline-block;
                                    background: #fef2f2;
                                    border: 2px solid #fca5a5;
                                    color: #dc2626;
                                    padding: 10px 25px;
                                    border-radius: 50px;
                                    font-weight: bold;
                                    font-size: 15px;
                                    margin-bottom: 30px;
                                }
                                .info {
                                    background: #f9fafb;
                                    border-radius: 10px;
                                    padding: 15px 20px;
                                    color: #374151;
                                    font-size: 14px;
                                    border-left: 4px solid #f59e0b;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <div class="icon">❌</div>
                                <h1>Lien invalide</h1>
                                <p>Ce lien de vérification est <strong>invalide ou expiré</strong>.</p>
                                <div class="badge">⏰ Le lien expire après 24 heures</div>
                                <div class="info">
                                    Retournez sur l'application JobNest<br>
                                    et demandez un nouveau lien de vérification.
                                </div>
                            </div>
                        </body>
                        </html>
                        """;
                }

                byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("🌐 Serveur de vérification démarré sur http://localhost:" + port);

        } catch (Exception e) {
            System.err.println("❌ Impossible de démarrer le serveur : " + e.getMessage());
        }
    }

    /**
     * Retourne le port utilisé — utile pour générer le bon lien dans l'email.
     */
    public static int getPort() {
        return port;
    }

    /**
     * Arrête proprement le serveur.
     */
    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Serveur de vérification arrêté.");
        }
    }
}