package com.jarvis.skills.greet;

import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class GreetSkill implements Skill {

    private static final Random RNG = new Random();

    private static final List<String> MORNING = List.of(
            "Good morning. All systems are fully operational.",
            "Good morning. Ready to assist you.",
            "Morning. J.A.R.V.I.S. online and standing by."
    );
    private static final List<String> AFTERNOON = List.of(
            "Good afternoon. How can I be of service?",
            "Good afternoon. All systems nominal.",
            "Afternoon. What do you need?"
    );
    private static final List<String> EVENING = List.of(
            "Good evening. Systems online. What can I do for you?",
            "Good evening. Ready and awaiting your instructions.",
            "Evening. J.A.R.V.I.S. at your service."
    );

    @Override
    public String getIntentName() {
        return "GREET";
    }

    @Override
    public String execute(Map<String, String> entities) {
        int hour = LocalTime.now().getHour();
        List<String> pool = hour < 12 ? MORNING : hour < 18 ? AFTERNOON : EVENING;
        return pool.get(RNG.nextInt(pool.size()));
    }
}
