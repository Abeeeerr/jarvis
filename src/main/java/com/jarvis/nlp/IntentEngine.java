package com.jarvis.nlp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntentEngine {

    public enum Intent {
        // Time & Date
        TIME, DATE,
        // Music
        MUSIC_PLAY, MUSIC_STOP,
        // Social
        GREET,
        // Knowledge
        WEB_SEARCH, WIKIPEDIA, WEATHER,
        // Productivity
        NOTE_TAKE, NOTE_READ, NOTE_LIST,
        REMINDER_SET, REMINDER_LIST,
        // Utility
        JOKE, CALCULATOR,
        // AI-powered
        HOMEWORK_HELP, CODING_HELP, GENERAL_CHAT,
        // System / OS
        APP_OPEN,
        // Fallback
        UNKNOWN
    }

    @Autowired
    private GemmaService gemmaService;

    public Intent detectIntent(String text) {
        if (text == null || text.isBlank()) return Intent.UNKNOWN;
        return gemmaService.analyze(text).getIntent();
    }
}
