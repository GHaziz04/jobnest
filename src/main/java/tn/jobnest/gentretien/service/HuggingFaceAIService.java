package tn.jobnest.gentretien.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HuggingFaceAIService {



    private static final String MODEL_URL =
            "https://router.huggingface.co/hf-inference/models/Jean-Baptiste/roberta-large-ner-english";

    public List<String> extractSkills(String description) {

        List<String> skills = new ArrayList<>();

        try {
            URL url = new URL(MODEL_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("inputs", description);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("HF Response Code: " + responseCode);

            InputStream stream = (responseCode == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            String jsonResponse = response.toString();
            System.out.println("HF RAW RESPONSE: " + jsonResponse);

            if (responseCode == 200 && jsonResponse.startsWith("[")) {

                JSONArray array = new JSONArray(jsonResponse);

                for (int i = 0; i < array.length(); i++) {

                    JSONObject obj = array.getJSONObject(i);
                    String word   = obj.optString("word", "");
                    String entity = obj.optString("entity_group", "");

                    if (entity.equalsIgnoreCase("ORG")
                            || entity.equalsIgnoreCase("MISC")
                            || entity.equalsIgnoreCase("PRODUCT")) {

                        String cleanWord = word.replace("##", "").trim();
                        String[] parts   = cleanWord.split("[\\n,]");

                        for (String part : parts) {
                            String finalSkill = part.trim();
                            if (finalSkill.length() > 2 && !skills.contains(finalSkill)) {
                                skills.add(finalSkill);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("AI Skills Cleaned: " + skills);
        return skills;
    }
}