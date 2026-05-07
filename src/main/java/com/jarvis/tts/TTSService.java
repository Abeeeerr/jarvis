package com.jarvis.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TTSService — speaks text aloud using the macOS `say` command.
 *
 * Voice: Daniel (en_GB) — British male, calm and authoritative, closest to
 * the JARVIS voice from the Iron Man films.
 *
 * Tune via application.properties:
 *   jarvis.tts.voice=Daniel   (run `say -v ?` for the full list)
 *   jarvis.tts.rate=160       (words per minute; default macOS is 175)
 */
@Service
public class TTSService {

    private static final Logger log = Logger.getLogger(TTSService.class.getName());

    @Value("${jarvis.tts.voice:Daniel}")
    private String voice;

    @Value("${jarvis.tts.rate:160}")
    private int rate;

    private final boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");

    public void speak(String text) {
        if (text == null || text.isBlank()) return;
        System.out.println("[Jarvis] " + text);
        if (isMac) speakViaSay(text);
    }

    private void speakViaSay(String text) {
        try {
            String safe = text.replace("\"", "\\\"");
            ProcessBuilder pb = new ProcessBuilder(
                    "say", "-v", voice, "-r", String.valueOf(rate), safe
            );
            pb.inheritIO();
            pb.start().waitFor();
        } catch (Exception e) {
            log.log(Level.WARNING, "macOS `say` command failed.", e);
        }
    }
}
