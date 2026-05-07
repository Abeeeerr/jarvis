package com.jarvis.skills.chat;

import com.jarvis.nlp.GemmaService;
import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HomeworkSkill implements Skill {

    @Autowired private GemmaService gemma;

    private static final String SYSTEM = """
You are Jarvis, an expert tutor. Explain concepts clearly and concisely in 2-4 sentences.
Use simple language first, then add detail if needed. Give examples where helpful.
If it's a math or science problem, show the steps. Never give just an answer without explanation.
Keep responses spoken-friendly — no markdown, no bullet points, just clear sentences.
""";

    @Override public String getIntentName() { return "HOMEWORK_HELP"; }

    @Override
    public String execute(Map<String, String> entities) {
        String question = entities.getOrDefault("question",
                entities.getOrDefault("original_text", "")).trim();
        if (question.isBlank()) return "What subject or question can I help you with?";
        String response = gemma.ask(SYSTEM, question);
        return response.isBlank() ? "I couldn't work that out. Try rephrasing your question." : response;
    }
}
