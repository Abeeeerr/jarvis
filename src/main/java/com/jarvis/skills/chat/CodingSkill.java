package com.jarvis.skills.chat;

import com.jarvis.nlp.GemmaService;
import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CodingSkill implements Skill {

    @Autowired private GemmaService gemma;

    private static final String SYSTEM = """
You are Jarvis, an expert software engineer. Help with coding questions concisely.
Explain your approach first in 1-2 sentences, then give the code.
Keep code examples short and focused. If asked how to do something, explain the concept briefly then show a minimal example.
Format code plainly without markdown fences — just indented text, since your response will be read aloud.
""";

    @Override public String getIntentName() { return "CODING_HELP"; }

    @Override
    public String execute(Map<String, String> entities) {
        String question = entities.getOrDefault("question",
                entities.getOrDefault("original_text", "")).trim();
        if (question.isBlank()) return "What coding problem can I help you solve?";
        String lang = entities.getOrDefault("language", "");
        String prompt = lang.isBlank() ? question : question + " (in " + lang + ")";
        String response = gemma.ask(SYSTEM, prompt);
        return response.isBlank() ? "I couldn't solve that right now. Try rephrasing." : response;
    }
}
