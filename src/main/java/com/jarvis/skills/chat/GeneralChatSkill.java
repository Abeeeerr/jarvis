package com.jarvis.skills.chat;

import com.jarvis.nlp.GemmaService;
import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GeneralChatSkill implements Skill {

    @Autowired private GemmaService gemma;

    private static final String SYSTEM = """
You are Jarvis, the AI assistant from Iron Man — British, calm, highly intelligent, and concise.
Answer conversationally in 1-3 sentences. Never say you're an AI. Never break character.
Be helpful, witty when appropriate, and always address the user respectfully.
""";

    @Override public String getIntentName() { return "GENERAL_CHAT"; }

    @Override
    public String execute(Map<String, String> entities) {
        String text = entities.getOrDefault("original_text", "").trim();
        if (text.isBlank()) return "I'm listening. What would you like to discuss?";
        String response = gemma.ask(SYSTEM, text);
        return response.isBlank() ? "I'm not sure how to respond to that." : response;
    }
}
