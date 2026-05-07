package com.jarvis.skills.reminder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jarvis.skills.Skill;
import com.jarvis.tts.TTSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReminderSkill implements Skill {

    private static final Logger log = Logger.getLogger(ReminderSkill.class.getName());
    private static final File REMINDERS_FILE = new File(System.getProperty("user.home") + "/.jarvis/reminders.json");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired private TTSService tts;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override public String getIntentName() { return "REMINDER_SET"; }

    @Override
    public String execute(Map<String, String> entities) {
        String intent = entities.getOrDefault("intent", "REMINDER_SET").toUpperCase();
        if (intent.equals("REMINDER_LIST")) return listReminders();

        String task     = entities.getOrDefault("task", "").trim();
        String timeStr  = entities.getOrDefault("time", "").trim();
        String original = entities.getOrDefault("original_text", "").trim();

        // Parse time from entities or extract from original text
        LocalDateTime when = parseTime(timeStr.isBlank() ? original : timeStr);
        if (task.isBlank()) task = extractTask(original);
        if (task.isBlank()) return "What would you like me to remind you about?";
        if (when == null)   return "I got it — remind you to " + task + ". But what time should I remind you?";

        List<Reminder> reminders = load();
        reminders.add(new Reminder(reminders.size() + 1, task, when, false));
        save(reminders);

        return "Reminder set. I'll remind you to " + task + " at " + when.format(TIME_FMT) + ".";
    }

    private String listReminders() {
        List<Reminder> reminders = load().stream().filter(r -> !r.triggered()).toList();
        if (reminders.isEmpty()) return "You have no upcoming reminders.";
        StringBuilder sb = new StringBuilder("You have " + reminders.size() + " reminder" + (reminders.size() > 1 ? "s" : "") + ". ");
        for (Reminder r : reminders) {
            sb.append("Remind you to ").append(r.task()).append(" at ").append(r.when().format(TIME_FMT)).append(". ");
        }
        return sb.toString();
    }

    /** Checks every 30 seconds for due reminders and speaks them aloud. */
    @Scheduled(fixedDelay = 30_000)
    public void checkReminders() {
        List<Reminder> reminders = load();
        boolean changed = false;
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            if (!r.triggered() && LocalDateTime.now().isAfter(r.when())) {
                tts.speak("Reminder: " + r.task());
                reminders.set(i, new Reminder(r.id(), r.task(), r.when(), true));
                changed = true;
            }
        }
        if (changed) save(reminders);
    }

    // ── Time parsing ──────────────────────────────────────────────────────────

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.toLowerCase();

        // "at 3pm", "at 3:30 pm", "at 15:00"
        Pattern p = Pattern.compile("(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");
        Matcher m = p.matcher(s);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            int min  = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            String ampm = m.group(3);
            if ("pm".equals(ampm) && hour < 12) hour += 12;
            if ("am".equals(ampm) && hour == 12) hour = 0;
            LocalTime t = LocalTime.of(hour % 24, min);
            LocalDateTime dt = LocalDateTime.now().with(t);
            if (dt.isBefore(LocalDateTime.now())) dt = dt.plusDays(1);
            return dt;
        }
        return null;
    }

    private String extractTask(String text) {
        return text.replaceAll("(?i)remind me (to|about)?|set a? reminder (to|for)?|at \\d.*", "").trim();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private List<Reminder> load() {
        try {
            REMINDERS_FILE.getParentFile().mkdirs();
            if (!REMINDERS_FILE.exists()) return new ArrayList<>();
            return mapper.readValue(REMINDERS_FILE, new TypeReference<>() {});
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private void save(List<Reminder> list) {
        try { REMINDERS_FILE.getParentFile().mkdirs(); mapper.writeValue(REMINDERS_FILE, list); }
        catch (Exception e) { log.warning("Reminder save failed: " + e.getMessage()); }
    }

    public record Reminder(int id, String task, LocalDateTime when, boolean triggered) {}
}
