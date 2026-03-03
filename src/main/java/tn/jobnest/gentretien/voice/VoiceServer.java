package tn.jobnest.gentretien.voice;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;

public class VoiceServer {

    private static HttpServer server;

    public static void start() {

        try {

            if(server != null) return;

            server = HttpServer.create(new InetSocketAddress(5555), 0);

            // ================= SERVE HTML =================
            server.createContext("/voice.html", exchange -> {

                InputStream is = VoiceServer.class
                        .getClassLoader()
                        .getResourceAsStream("voice.html");

                if(is == null){
                    String resp = "voice.html NOT FOUND";
                    exchange.sendResponseHeaders(404, resp.length());
                    exchange.getResponseBody().write(resp.getBytes());
                    exchange.close();
                    return;
                }

                byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });

            // ================= RECEIVE TEXT =================
            server.createContext("/voice", exchange -> {

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(exchange.getRequestBody()));

                String text = reader.readLine();

                VoiceBridge.dispatch(text);

                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });

            server.start();
            System.out.println("VOICE SERVER STARTED → http://localhost:5555/voice.html");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // إيقاف السيرفر عند غلق التطبيق
    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("VOICE SERVER STOPPED");
        }
    }


}