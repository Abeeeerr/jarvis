package com.jarvis.skills.reminder;

import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReminderListSkill implements Skill {

    @Autowired private ReminderSkill reminderSkill;

    @Override public String getIntentName() { return "REMINDER_LIST"; }

    @Override
    public String execute(Map<String, String> entities) {
        Map<String, String> adapted = new java.util.HashMap<>(entities);
        adapted.put("intent", "REMINDER_LIST");
        return reminderSkill.execute(adapted);
    }
}
