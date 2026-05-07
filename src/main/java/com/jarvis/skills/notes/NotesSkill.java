package com.jarvis.skills.notes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class NotesSkill implements Skill {

    private static final Logger log = Logger.getLogger(NotesSkill.class.getName());
    private static final File NOTES_FILE = new File(System.getProperty("user.home") + "/.jarvis/notes.json");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override public String getIntentName() { return "NOTE_TAKE"; }

    @Override
    public String execute(Map<String, String> entities) {
        String intent  = entities.getOrDefault("intent", "NOTE_TAKE").toUpperCase();
        String content = entities.getOrDefault("content",
                entities.getOrDefault("original_text", "")).trim();

        return switch (intent) {
            case "NOTE_READ", "NOTE_LIST" -> listNotes();
            default -> takeNote(content);
        };
    }

    private String takeNote(String content) {
        if (content.isBlank()) return "What would you like me to note down?";
        content = content.replaceAll("(?i)^(take a note|note that|remember that|write down)\\s*", "").trim();
        if (content.isBlank()) return "I didn't catch what to note. Please say what to write down.";

        List<Note> notes = load();
        notes.add(new Note(notes.size() + 1, content, LocalDateTime.now()));
        save(notes);
        return "Noted. I've saved: \"" + content + "\".";
    }

    private String listNotes() {
        List<Note> notes = load();
        if (notes.isEmpty()) return "You have no saved notes yet.";
        StringBuilder sb = new StringBuilder("You have " + notes.size() + " note" + (notes.size() > 1 ? "s" : "") + ". ");
        int show = Math.min(notes.size(), 5);
        for (int i = notes.size() - show; i < notes.size(); i++) {
            Note n = notes.get(i);
            sb.append("Note ").append(n.id).append(": ").append(n.content)
              .append(", saved on ").append(n.timestamp.format(FMT)).append(". ");
        }
        if (notes.size() > 5) sb.append("And ").append(notes.size() - 5).append(" more.");
        return sb.toString();
    }

    private List<Note> load() {
        try {
            NOTES_FILE.getParentFile().mkdirs();
            if (!NOTES_FILE.exists()) return new ArrayList<>();
            return mapper.readValue(NOTES_FILE, new TypeReference<>() {});
        } catch (Exception e) { log.warning("Notes load failed: " + e.getMessage()); return new ArrayList<>(); }
    }

    private void save(List<Note> notes) {
        try { NOTES_FILE.getParentFile().mkdirs(); mapper.writeValue(NOTES_FILE, notes); }
        catch (Exception e) { log.warning("Notes save failed: " + e.getMessage()); }
    }

    public record Note(int id, String content, LocalDateTime timestamp) {}
}
