package com.jarvis.skills.web;

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
public class WebSearchSkill implements Skill {

    private static final Logger log = Logger.getLogger(WebSearchSkill.class.getName());
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override public String getIntentName() { return "WEB_SEARCH"; }

    @Override
    public String execute(Map<String, String> entities) {
        String query = entities.getOrDefault("query",
                entities.getOrDefault("original_text", "")).trim();
        if (query.isBlank()) return "What would you like me to search for?";

        // Open Chrome with a real Google search — this is what the user actually asked for
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String googleUrl = "https://www.google.com/search?q=" + encoded;
        openInChrome(googleUrl);

        // Also fetch a quick spoken answer from DuckDuckGo in the background
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1"))
                    .header("User-Agent", "Jarvis/1.0")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(res.body());

            String abstract_ = root.path("AbstractText").asText("").trim();
            if (!abstract_.isBlank()) {
                return "Searching for " + query + " in Chrome. Here's a quick answer: " + truncate(abstract_, 250);
            }

            JsonNode topics = root.path("RelatedTopics");
            if (topics.isArray() && topics.size() > 0) {
                String snippet = topics.get(0).path("Text").asText("").trim();
                if (!snippet.isBlank()) {
                    return "Opening Chrome with your search. I also found: " + truncate(snippet, 250);
                }
            }
        } catch (Exception e) {
            log.warning("DuckDuckGo fetch failed: " + e.getMessage());
        }

        return "Opening Chrome and searching for " + query + ".";
    }

    private void openInChrome(String url) {
        // Try Chrome first, then fall back to the system default browser
        try {
            new ProcessBuilder("open", "-a", "Google Chrome", url).start();
            return;
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("open", url).start();
        } catch (Exception e) {
            log.warning("Could not open browser: " + e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max).trim() + "…" : s;
    }
}
