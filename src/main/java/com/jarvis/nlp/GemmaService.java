package com.jarvis.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GemmaService — one-stop NLP via Gemma 3 4B running locally in Ollama.
 * Detects intent + extracts entities in a single LLM call, with a same-
 * utterance cache so IntentEngine and EntityExtractor share one round-trip.
 */
@Service
public class GemmaService {

    private static final Logger log = Logger.getLogger(GemmaService.class.getName());

    @Value("${jarvis.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${jarvis.ollama.model:gemma3:4b}")
    private String model;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // Same-utterance cache — avoids two Ollama round-trips per command
    private String lastInput = null;
    private NlpResult lastResult = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public NlpResult analyze(String text) {
        if (text != null && text.equals(lastInput) && lastResult != null) return lastResult;
        lastInput  = text;
        lastResult = callOllama(text);
        return lastResult;
    }

    /** Direct call for skills that need Gemma as a general AI (chat, homework, coding). */
    public String ask(String systemPrompt, String userText) {
        String body;
        try {
            body = mapper.writeValueAsString(Map.of(
                    "model",  model,
                    "prompt", systemPrompt + "\n\nUser: " + userText + "\n\nJarvis:",
                    "stream", false,
                    "options", Map.of("temperature", 0.7)
            ));
        } catch (Exception e) { return "I had trouble thinking about that."; }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(res.body());
            return root.path("response").asText("").trim();
        } catch (Exception e) {
            log.warning("Gemma ask() failed: " + e.getMessage());
            return "I couldn't process that right now.";
        }
    }

    // -----------------------------------------------------------------------
    // Ollama NLP call
    // -----------------------------------------------------------------------

    private NlpResult callOllama(String text) {
        String body;
        try {
            body = mapper.writeValueAsString(Map.of(
                    "model",  model,
                    "prompt", buildPrompt(text),
                    "stream", false,
                    "format", "json",
                    "options", Map.of("temperature", 0.1)
            ));
        } catch (Exception e) { return fallback(text); }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return fallback(text);
            return parse(res.body(), text);
        } catch (Exception e) {
            log.log(Level.WARNING, "Ollama unreachable — keyword fallback.", e);
            return fallback(text);
        }
    }

    private NlpResult parse(String ollamaJson, String original) {
        try {
            JsonNode root     = mapper.readTree(ollamaJson);
            String response   = root.path("response").asText("{}").trim()
                    .replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            JsonNode parsed   = mapper.readTree(response);

            String intentStr  = parsed.path("intent").asText("UNKNOWN").toUpperCase();
            IntentEngine.Intent intent;
            try { intent = IntentEngine.Intent.valueOf(intentStr); }
            catch (IllegalArgumentException ex) { intent = IntentEngine.Intent.UNKNOWN; }

            Map<String, String> entities = new HashMap<>();
            JsonNode en = parsed.path("entities");
            if (en.isObject()) en.fields().forEachRemaining(e -> entities.put(e.getKey(), e.getValue().asText()));
            entities.put("original_text", original);

            return new NlpResult(intent, entities);
        } catch (Exception e) {
            log.warning("Could not parse Gemma response: " + ollamaJson);
            return fallback(original);
        }
    }

    // -----------------------------------------------------------------------
    // Prompt
    // -----------------------------------------------------------------------

    private String buildPrompt(String text) {
        return """
You are Jarvis, an AI assistant NLP classifier. Analyse the user's request and return ONLY a raw JSON object (no markdown, no explanation) with exactly two keys:
  "intent"   — one of the intent codes below
  "entities" — object of extracted values (empty object {} if none)

INTENT CODES & RULES
────────────────────
TIME          — "what time is it", "current time", "what's the hour"
DATE          — "what date is it", "what day is today", "today's date"
MUSIC_PLAY    — "play X", "put on X", "play some music"          → entities: {song_name, artist_name}
MUSIC_STOP    — "stop music", "pause", "stop playing"
GREET         — "hello", "hi", "hey jarvis", "good morning/evening"
WEB_SEARCH    — "search for X", "look up X", "google X", "find X online", "search X" → entities: {query}
WIKIPEDIA     — "what is X", "who is X", "tell me about X", "define X", "explain X"  → entities: {topic}
WEATHER       — "weather in X", "what's the weather", "temperature in X"              → entities: {location}
NOTE_TAKE     — "take a note", "note that X", "remember that X", "write down X"       → entities: {content}
NOTE_READ     — "read my notes", "what did I note", "show me my notes"
NOTE_LIST     — "list all notes", "how many notes", "my notes"
REMINDER_SET  — "remind me to X at Y", "set a reminder for X", "alert me when Y"     → entities: {task, time}
REMINDER_LIST — "show reminders", "what reminders do I have", "my reminders"
JOKE          — "tell me a joke", "say something funny", "make me laugh"
CALCULATOR    — "what is X plus/minus/times Y", "calculate X", "how much is X"        → entities: {expression}
HOMEWORK_HELP — "help me with X", "explain how X works", "what is the formula for X", "homework" → entities: {question}
CODING_HELP   — "write code for X", "how do I code X", "debug X", "write a function", "python", "java", "javascript" → entities: {question, language}
APP_OPEN      — "open Chrome", "launch Spotify", "open VS Code", "start Firefox", "open Finder", "open [any app name]" → entities: {app_name}
GENERAL_CHAT  — opinions, recommendations, small talk, anything conversational
UNKNOWN       — cannot classify

User said: "%s"

JSON only:
""".formatted(text);
    }

    // -----------------------------------------------------------------------
    // Keyword fallback (used when Ollama is unreachable)
    // -----------------------------------------------------------------------

    private NlpResult fallback(String text) {
        if (text == null) return new NlpResult(IntentEngine.Intent.UNKNOWN, Map.of("original_text", ""));
        String l = text.toLowerCase();
        Map<String, String> e = new HashMap<>();
        e.put("original_text", text);

        IntentEngine.Intent intent;
        if (l.matches("(?i)(open|launch|start|run)\\s+.+"))         { intent = IntentEngine.Intent.APP_OPEN; e.put("app_name", l.replaceAll("(?i)^(open|launch|start|run)\\s+", "").trim()); }
        else if (l.contains("time") || l.contains("clock"))         intent = IntentEngine.Intent.TIME;
        else if (l.contains("date") || l.contains("day is"))        intent = IntentEngine.Intent.DATE;
        else if (l.contains("stop") || l.contains("pause"))         intent = IntentEngine.Intent.MUSIC_STOP;
        else if (l.contains("play") || l.contains("music"))         { intent = IntentEngine.Intent.MUSIC_PLAY; extractMusic(l, e); }
        else if (l.contains("weather") || l.contains("temperature")) intent = IntentEngine.Intent.WEATHER;
        else if (l.contains("search") || l.contains("look up") || l.contains("google")) { intent = IntentEngine.Intent.WEB_SEARCH; e.put("query", text.replaceAll("(?i)(search\\s*(for)?|look\\s*up|google)\\s*", "").trim()); }
        else if (l.contains("note"))                                 intent = IntentEngine.Intent.NOTE_TAKE;
        else if (l.contains("remind"))                               intent = IntentEngine.Intent.REMINDER_SET;
        else if (l.contains("joke"))                                 intent = IntentEngine.Intent.JOKE;
        else if (l.contains("hello") || l.contains("hi") || l.contains("hey")) intent = IntentEngine.Intent.GREET;
        else if (l.contains("code") || l.contains("function") || l.contains("program")) { intent = IntentEngine.Intent.CODING_HELP; e.put("question", text); }
        else if (l.contains("help") || l.contains("explain") || l.contains("what is")) { intent = IntentEngine.Intent.HOMEWORK_HELP; e.put("question", text); }
        else                                                         { intent = IntentEngine.Intent.GENERAL_CHAT; }

        return new NlpResult(intent, e);
    }

    private void extractMusic(String l, Map<String, String> e) {
        int i = l.indexOf("play");
        if (i >= 0 && i + 5 < l.length()) {
            String after = l.substring(i + 5).trim();
            int by = after.indexOf(" by ");
            if (by >= 0) { e.put("song_name", after.substring(0, by).trim()); e.put("artist_name", after.substring(by + 4).trim()); }
            else e.put("song_name", after);
        }
    }
}
