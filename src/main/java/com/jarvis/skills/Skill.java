package com.jarvis.skills;

import java.util.Map;

/**
 * Skill — contract that every Jarvis capability must implement.
 *
 * A skill is a self-contained unit of functionality (e.g. telling the time,
 * playing music) that is registered by {@link SkillRegistry} and invoked by
 * {@link com.jarvis.core.SkillDispatcher}.
 *
 * Implementing classes should be annotated with {@code @Component} so that
 * Spring registers them automatically and {@link SkillRegistry} can collect
 * them via {@code @Autowired List<Skill>}.
 */
public interface Skill {

    /**
     * Returns the intent name this skill handles.
     *
     * The value must match the {@link com.jarvis.nlp.IntentEngine.Intent}
     * enum name (e.g. {@code "TIME"}, {@code "MUSIC_PLAY"}).
     * A skill may handle multiple intents by implementing the interface
     * multiple times or by registering itself under several keys — see
     * {@link SkillRegistry#register(Skill)} for how multi-intent skills
     * can advertise themselves.
     *
     * @return the intent name string, never {@code null}.
     */
    String getIntentName();

    /**
     * Executes the skill and returns a human-readable response.
     *
     * The {@code entities} map contains key/value pairs extracted by
     * {@link com.jarvis.nlp.EntityExtractor}, plus an {@code "intent"} key
     * added by the dispatcher so that skills handling multiple intents
     * can branch on it.
     *
     * @param entities entity map (may be empty but never {@code null}).
     * @return the response string that will be spoken to the user.
     */
    String execute(Map<String, String> entities);
}
