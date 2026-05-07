package com.jarvis.skills;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SkillRegistry — central directory of all available {@link Skill} beans.
 *
 * On startup Spring injects every bean that implements {@link Skill} via the
 * {@code allSkills} list.  {@link #init()} then walks that list and calls
 * {@link #register(Skill)} for each one, so the dispatcher can do an O(1)
 * lookup by intent name.
 *
 * To add a new skill, simply:
 * <ol>
 *   <li>Create a class that implements {@link Skill}.</li>
 *   <li>Annotate it with {@code @Component}.</li>
 *   <li>Set {@link Skill#getIntentName()} to the relevant intent string.</li>
 * </ol>
 * No other wiring is required — Spring and this registry do the rest.
 */
@Component
public class SkillRegistry {

    private static final Logger log = Logger.getLogger(SkillRegistry.class.getName());

    /** All {@link Skill} beans discovered by Spring at startup. */
    @Autowired
    private List<Skill> allSkills;

    /**
     * Internal map: intent name (upper-cased) → {@link Skill} implementation.
     * Uses a {@link HashMap} for O(1) lookup.
     */
    private final Map<String, Skill> registry = new HashMap<>();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Registers all auto-discovered skills after dependency injection.
     */
    @PostConstruct
    public void init() {
        if (allSkills == null || allSkills.isEmpty()) {
            log.warning("No Skill beans found — the assistant will respond to no intents.");
            return;
        }
        for (Skill skill : allSkills) {
            register(skill);
        }
        log.info("SkillRegistry initialised with " + registry.size() + " skill(s): " + registry.keySet());
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Registers a skill under its declared intent name.
     *
     * If a skill is already registered for that intent, the new one replaces
     * the old one and a warning is logged.
     *
     * @param skill the skill to register (must not be {@code null}).
     */
    public void register(Skill skill) {
        if (skill == null) {
            log.warning("Attempted to register a null skill — ignored.");
            return;
        }
        String key = normalise(skill.getIntentName());
        if (registry.containsKey(key)) {
            log.warning("Overwriting existing skill for intent '" + key + "' with " + skill.getClass().getSimpleName());
        }
        registry.put(key, skill);
        log.info("Registered skill: " + skill.getClass().getSimpleName() + " → " + key);
    }

    /**
     * Looks up the skill for the given intent name.
     *
     * @param intent the intent name (case-insensitive).
     * @return an {@link Optional} containing the skill, or empty if none found.
     */
    public Optional<Skill> getSkill(String intent) {
        return Optional.ofNullable(registry.get(normalise(intent)));
    }

    /**
     * Returns {@code true} if at least one skill is registered for the intent.
     *
     * @param intent the intent name (case-insensitive).
     */
    public boolean hasSkill(String intent) {
        return registry.containsKey(normalise(intent));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String normalise(String intent) {
        return intent == null ? "" : intent.toUpperCase().trim();
    }
}
