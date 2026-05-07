package com.jarvis.nlp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * EntityExtractor — pulls named entities out of a transcribed utterance.
 * Delegates to GemmaService (Gemma 3 via Ollama) with a keyword fallback.
 * Because GemmaService caches the last result, this shares the same LLM
 * call that IntentEngine already made for the same utterance.
 */
@Service
public class EntityExtractor {

    public static final String ENTITY_SONG_NAME   = "song_name";
    public static final String ENTITY_ARTIST_NAME = "artist_name";

    @Autowired
    private GemmaService gemmaService;

    public Map<String, String> extract(String text, IntentEngine.Intent intent) {
        if (text == null || text.isBlank()) return Map.of();
        return gemmaService.analyze(text).getEntities();
    }
}
