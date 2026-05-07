package com.jarvis.nlp;

import java.util.Collections;
import java.util.Map;

public class NlpResult {

    private final IntentEngine.Intent intent;
    private final Map<String, String> entities;

    public NlpResult(IntentEngine.Intent intent, Map<String, String> entities) {
        this.intent = intent;
        this.entities = entities == null ? Collections.emptyMap() : entities;
    }

    public IntentEngine.Intent getIntent() { return intent; }
    public Map<String, String> getEntities() { return entities; }

    @Override
    public String toString() {
        return "NlpResult{intent=" + intent + ", entities=" + entities + "}";
    }
}
