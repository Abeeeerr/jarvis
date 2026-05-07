package com.jarvis.controller;

import com.jarvis.core.SkillDispatcher;
import com.jarvis.nlp.EntityExtractor;
import com.jarvis.nlp.IntentEngine;
import com.jarvis.nlp.IntentEngine.Intent;
import com.jarvis.skills.SkillRegistry;
import com.jarvis.skills.music.MusicSkill;
import com.jarvis.stt.VoskSTTService;
import com.jarvis.tts.TTSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.logging.Logger;

/**
 * VoiceController — orchestrates the complete voice-assistant pipeline.
 *
 * Pipeline for each user utterance:
 * <pre>
 *   Microphone
 *     └─► VoskSTTService.startListening()   (speech → text)
 *           └─► IntentEngine.detectIntent()  (text → intent enum)
 *                 └─► EntityExtractor.extract()  (text + intent → entities)
 *                       └─► SkillDispatcher.dispatch()  (intent + entities → response)
 *                             └─► TTSService.speak()  (response → audio)
 * </pre>
 *
 * The {@link #run()} method loops indefinitely, printing a prompt before each
 * listen cycle and gracefully handling empty transcriptions.
 */
@Component
public class VoiceController {

    private static final Logger log = Logger.getLogger(VoiceController.class.getName());

    @Value("${jarvis.name:Jarvis}")
    private String assistantName;

    @Autowired
    private VoskSTTService sttService;

    @Autowired
    private IntentEngine intentEngine;

    @Autowired
    private EntityExtractor entityExtractor;

    @Autowired
    private SkillDispatcher skillDispatcher;

    @Autowired
    private TTSService ttsService;

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private MusicSkill musicSkill;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Registers MusicSkill under its secondary intent (MUSIC_STOP) after all
     * beans are wired.  MusicSkill's primary intent (MUSIC_PLAY) is picked up
     * automatically by SkillRegistry via the allSkills list.
     */
    @PostConstruct
    public void registerSecondaryIntents() {
        // SkillRegistry#init() fires before this (both are @PostConstruct, but
        // Spring processes @Autowired fields first, then calls @PostConstruct in
        // bean creation order).  We register MUSIC_STOP explicitly here to
        // guarantee it is always mapped regardless of bean ordering.
        skillRegistry.register(new MusicSkillStopAdapter(musicSkill));
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Processes a single voice command through the full pipeline.
     *
     * @return the response string that was spoken, or an empty string if the
     *         STT returned no text.
     */
    public String processCommand() {
        // Step 1 — Speech-to-Text
        System.out.println(assistantName + " is listening...");
        String transcribedText = sttService.startListening();

        if (transcribedText.isBlank()) {
            log.fine("STT returned empty text — skipping pipeline.");
            return "";
        }

        System.out.println("[You said] " + transcribedText);

        // Step 2 — Intent Detection
        Intent intent = intentEngine.detectIntent(transcribedText);
        log.info("Detected intent: " + intent);

        // Step 3 — Entity Extraction
        Map<String, String> entities = entityExtractor.extract(transcribedText, intent);
        log.info("Extracted entities: " + entities);

        // Step 4 — Skill Dispatch
        String response = skillDispatcher.dispatch(intent, entities);

        // Step 5 — Text-to-Speech
        ttsService.speak(response);

        return response;
    }

    /**
     * Runs the voice assistant in an infinite loop.
     *
     * The loop continues until the JVM is terminated (e.g. Ctrl+C).
     * Each iteration processes one complete voice command.
     */
    public void run() {
        System.out.println(assistantName + " is ready. Use the web UI at http://localhost:8080");

        while (true) {
            try {
                String result = processCommand();
                // If STT returned nothing (mic unavailable), pause to avoid busy-spinning.
                if (result.isBlank()) Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warning("Unexpected error in command loop: " + e.getMessage());
                try { Thread.sleep(1_000); } catch (InterruptedException ie) { break; }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Inner adapter — lets MusicSkill handle MUSIC_STOP under a different key
    // -----------------------------------------------------------------------

    /**
     * Thin adapter that wraps {@link MusicSkill} and advertises the
     * {@code MUSIC_STOP} intent name so the registry maps it correctly.
     */
    private static class MusicSkillStopAdapter implements com.jarvis.skills.Skill {

        private final MusicSkill delegate;

        MusicSkillStopAdapter(MusicSkill delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getIntentName() {
            return "MUSIC_STOP";
        }

        @Override
        public String execute(Map<String, String> entities) {
            return delegate.execute(entities);
        }
    }
}
