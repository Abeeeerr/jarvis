package com.jarvis.skills.music;

import com.jarvis.nlp.EntityExtractor;
import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MusicSkill — handles both {@code MUSIC_PLAY} and {@code MUSIC_STOP} intents.
 *
 * Because a single skill class handles two intents, it registers itself under
 * {@code MUSIC_PLAY} via {@link Skill#getIntentName()}, but also registers
 * itself for {@code MUSIC_STOP} directly in
 * {@link com.jarvis.skills.SkillRegistry}.  The dispatcher adds an
 * {@code "intent"} key to the entities map so this class knows which branch
 * to execute.
 *
 * Play behaviour (macOS):
 *   Launches the Music.app via {@code open -a Music}.
 *   If a song name is present it is printed but not searched automatically
 *   (deep-linking into Music.app requires the AppleScript bridge which is
 *   out of scope for this baseline implementation).
 *
 * Stop behaviour:
 *   Sends an AppleScript {@code pause} command to Music.app.
 */
@Component
public class MusicSkill implements Skill {

    private static final Logger log = Logger.getLogger(MusicSkill.class.getName());

    /** The intent key injected into the entities map by the dispatcher. */
    public static final String ENTITY_INTENT = "intent";

    // -----------------------------------------------------------------------
    // Skill contract
    // -----------------------------------------------------------------------

    /**
     * Primary intent — the registry also registers this bean under
     * {@code MUSIC_STOP} manually via a second {@code register()} call.
     */
    @Override
    public String getIntentName() {
        return "MUSIC_PLAY";
    }

    /**
     * Branches on the {@code "intent"} entity key to handle play vs. stop.
     *
     * @param entities map containing at minimum {@code "intent"}, and
     *                 optionally {@code "song_name"} and {@code "artist_name"}.
     */
    @Override
    public String execute(Map<String, String> entities) {
        String intent = entities.getOrDefault(ENTITY_INTENT, "MUSIC_PLAY").toUpperCase();

        return switch (intent) {
            case "MUSIC_STOP" -> handleStop();
            default           -> handlePlay(entities);
        };
    }

    // -----------------------------------------------------------------------
    // Play / Stop handlers
    // -----------------------------------------------------------------------

    private String handlePlay(Map<String, String> entities) {
        String songName   = entities.get(EntityExtractor.ENTITY_SONG_NAME);
        String artistName = entities.get(EntityExtractor.ENTITY_ARTIST_NAME);

        // Build a friendly confirmation message.
        StringBuilder response = new StringBuilder("Playing");

        if (songName != null && !songName.isBlank()) {
            response.append(" ").append(songName);
        }
        if (artistName != null && !artistName.isBlank()) {
            response.append(" by ").append(artistName);
        }
        if (songName == null && artistName == null) {
            response.append(" music");
        }
        response.append(".");

        // Attempt to launch Music.app on macOS.
        try {
            String[] cmd;
            if (songName != null && !songName.isBlank()) {
                // Tell Music.app to search for the track via AppleScript.
                String script = String.format(
                        "tell application \"Music\" to play (first track of playlist 1 whose name contains \"%s\")",
                        songName.replace("\"", "\\\"")
                );
                cmd = new String[]{"osascript", "-e", script};
            } else {
                // Just open the app.
                cmd = new String[]{"open", "-a", "Music"};
            }
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not launch Music.app — falling back to console-only response.", e);
        }

        return response.toString();
    }

    private String handleStop() {
        // Send pause command to Music.app via AppleScript.
        try {
            String[] cmd = {"osascript", "-e", "tell application \"Music\" to pause"};
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not pause Music.app.", e);
        }
        return "Music paused.";
    }
}
