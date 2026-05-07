package com.jarvis.skills.info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class WikipediaSkill implements Skill {

    private static final Logger log = Logger.getLogger(WikipediaSkill.class.getName());
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override public String getIntentName() { return "WIKIPEDIA"; }

    @Override
    public String execute(Map<String, String> entities) {
        String topic = entities.getOrDefault("topic",
                entities.getOrDefault("original_text", "")).trim();
        if (topic.isBlank()) return "What topic would you like me to look up?";

        // Strip common prefixes so Wikipedia search works better
        topic = topic.replaceAll("(?i)^(what is|who is|tell me about|define|explain)\\s+", "").trim();

        try {
            String encoded = URLEncoder.encode(topic, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded))
                    .header("User-Agent", "Jarvis/1.0 (educational voice assistant)")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 404) {
                return "I couldn't find a Wikipedia article for \"" + topic + "\". Try rephrasing.";
            }

            JsonNode root = mapper.readTree(res.body());
            String extract = root.path("extract").asText("").trim();

            if (extract.isBlank()) return "Wikipedia didn't have a summary for that.";

            // Return first 2 sentences
            String[] sentences = extract.split("(?<=[.!?])\\s+");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < Math.min(2, sentences.length); i++) {
                out.append(sentences[i]).append(" ");
            }
            return out.toString().trim();

        } catch (Exception e) {
            log.warning("Wikipedia lookup failed: " + e.getMessage());
            return "I couldn't reach Wikipedia right now.";
        }
    }
}
