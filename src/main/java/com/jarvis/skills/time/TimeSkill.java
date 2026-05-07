package com.jarvis.skills.time;

import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * TimeSkill — handles the {@code TIME} intent.
 *
 * Returns the current local time formatted as a natural-language sentence,
 * e.g. "The current time is 3:45 PM".
 */
@Component
public class TimeSkill implements Skill {

    /** Formatter: "3:45 PM" — no leading zero, 12-hour clock with AM/PM. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a");

    // -----------------------------------------------------------------------
    // Skill contract
    // -----------------------------------------------------------------------

    @Override
    public String getIntentName() {
        return "TIME";
    }

    /**
     * Returns the current time as a human-readable sentence.
     *
     * @param entities not used by this skill (may be empty).
     * @return a spoken response containing the current time.
     */
    @Override
    public String execute(Map<String, String> entities) {
        String time = LocalTime.now().format(FORMATTER);
        return "The current time is " + time + ".";
    }
}
