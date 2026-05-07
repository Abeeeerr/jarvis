package com.jarvis.api;

import com.jarvis.core.SkillDispatcher;
import com.jarvis.nlp.EntityExtractor;
import com.jarvis.nlp.IntentEngine;
import com.jarvis.nlp.IntentEngine.Intent;
import com.jarvis.tts.TTSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JarvisApiController — REST API consumed by the web frontend.
 *
 * POST /api/command   { "text": "play bohemian rhapsody" }
 *                     → { "input": "...", "intent": "...", "response": "..." }
 *
 * GET  /api/status    → { "status": "online", "model": "gemma3:4b", ... }
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class JarvisApiController {

    @Autowired private IntentEngine intentEngine;
    @Autowired private EntityExtractor entityExtractor;
    @Autowired private SkillDispatcher skillDispatcher;
    @Autowired private TTSService ttsService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/command")
    public Map<String, String> processCommand(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "").trim();
        if (text.isBlank()) {
            return Map.of("error", "Empty input");
        }

        // Push "thinking" state to all connected UI clients
        broadcast("thinking", text);

        Intent intent      = intentEngine.detectIntent(text);
        Map<String, String> entities = entityExtractor.extract(text, intent);
        String response    = skillDispatcher.dispatch(intent, entities);

        // Speak through macOS TTS (the Daniel voice)
        ttsService.speak(response);

        // Push final response to all connected UI clients
        broadcast("response", response);

        return Map.of(
                "input",    text,
                "intent",   intent.name(),
                "response", response,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "status",    "online",
                "assistant", "J.A.R.V.I.S.",
                "model",     "Gemma 3 4B via Ollama",
                "stt",       "Browser Web Speech API",
                "tts",       "Daniel — en_GB (macOS)"
        );
    }

    private void broadcast(String type, String content) {
        try {
            messagingTemplate.convertAndSend("/topic/jarvis",
                    Map.of("type", type, "content", content));
        } catch (Exception ignored) {}
    }
}
