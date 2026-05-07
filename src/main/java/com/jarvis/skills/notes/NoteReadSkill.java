package com.jarvis.skills.notes;

import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoteReadSkill implements Skill {

    @Autowired private NotesSkill notesSkill;

    @Override public String getIntentName() { return "NOTE_READ"; }

    @Override
    public String execute(Map<String, String> entities) {
        Map<String, String> adapted = new java.util.HashMap<>(entities);
        adapted.put("intent", "NOTE_READ");
        return notesSkill.execute(adapted);
    }
}
