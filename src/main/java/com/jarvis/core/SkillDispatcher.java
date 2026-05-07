package com.jarvis.core;

import com.jarvis.nlp.IntentEngine;
import com.jarvis.skills.Skill;
import com.jarvis.skills.SkillRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

@Component
public class SkillDispatcher {

    private static final Logger log = Logger.getLogger(SkillDispatcher.class.getName());
    private static final Random RNG = new Random();

    private static final String[] UNKNOWN_RESPONSES = {
            "I'm not sure I follow. Could you rephrase that?",
            "That's outside my current capabilities, I'm afraid.",
            "I didn't quite catch what you need. Try asking again.",
            "I can help with the time, playing music, and a few other things. What would you like?"
    };

    @Autowired
    private SkillRegistry skillRegistry;

    public String dispatch(IntentEngine.Intent intent, Map<String, String> entities) {
        if (intent == null || intent == IntentEngine.Intent.UNKNOWN) {
            return randomUnknown();
        }

        Map<String, String> enriched = new HashMap<>(entities);
        enriched.put("intent", intent.name());

        Optional<Skill> skillOpt = skillRegistry.getSkill(intent.name());
        if (skillOpt.isEmpty()) {
            log.warning("No skill registered for intent: " + intent.name());
            return randomUnknown();
        }

        Skill skill = skillOpt.get();
        try {
            return skill.execute(enriched);
        } catch (Exception e) {
            log.warning("Skill threw an exception: " + e.getMessage());
            return "Sorry, something went wrong with that request.";
        }
    }

    private String randomUnknown() {
        return UNKNOWN_RESPONSES[RNG.nextInt(UNKNOWN_RESPONSES.length)];
    }
}
