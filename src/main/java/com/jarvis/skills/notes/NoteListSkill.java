package com.jarvis.skills.notes;

import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Adapter so NOTE_LIST and NOTE_READ intents route to NotesSkill. */
@Component
public class NoteListSkill implements Skill {

    @Autowired private NotesSkill notesSkill;

    @Override public String getIntentName() { return "NOTE_LIST"; }

    @Override
    public String execute(Map<String, String> entities) {
        Map<String, String> adapted = new java.util.HashMap<>(entities);
        adapted.put("intent", "NOTE_LIST");
        return notesSkill.execute(adapted);
    }
}
