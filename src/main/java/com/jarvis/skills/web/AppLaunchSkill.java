package com.jarvis.skills.web;

import com.jarvis.skills.Skill;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;

/**
 * AppLaunchSkill — opens macOS applications by name.
 * Uses the macOS `open -a` command, so any installed app works.
 * Handles intents like "open Chrome", "launch Spotify", "open VS Code".
 */
@Component
public class AppLaunchSkill implements Skill {

    private static final Logger log = Logger.getLogger(AppLaunchSkill.class.getName());

    // Common aliases the user might say → actual macOS app name
    private static final Map<String, String> APP_ALIASES = Map.ofEntries(
        Map.entry("chrome",        "Google Chrome"),
        Map.entry("google chrome", "Google Chrome"),
        Map.entry("firefox",       "Firefox"),
        Map.entry("safari",        "Safari"),
        Map.entry("spotify",       "Spotify"),
        Map.entry("vscode",        "Visual Studio Code"),
        Map.entry("vs code",       "Visual Studio Code"),
        Map.entry("visual studio code", "Visual Studio Code"),
        Map.entry("terminal",      "Terminal"),
        Map.entry("iterm",         "iTerm"),
        Map.entry("iterm2",        "iTerm"),
        Map.entry("finder",        "Finder"),
        Map.entry("notes",         "Notes"),
        Map.entry("calendar",      "Calendar"),
        Map.entry("mail",          "Mail"),
        Map.entry("slack",         "Slack"),
        Map.entry("zoom",          "Zoom"),
        Map.entry("whatsapp",      "WhatsApp"),
        Map.entry("telegram",      "Telegram"),
        Map.entry("discord",       "Discord"),
        Map.entry("xcode",         "Xcode"),
        Map.entry("intellij",      "IntelliJ IDEA"),
        Map.entry("idea",          "IntelliJ IDEA"),
        Map.entry("photoshop",     "Adobe Photoshop"),
        Map.entry("figma",         "Figma"),
        Map.entry("vlc",           "VLC"),
        Map.entry("youtube",       "")  // open in browser instead
    );

    @Override public String getIntentName() { return "APP_OPEN"; }

    @Override
    public String execute(Map<String, String> entities) {
        String appRaw = entities.getOrDefault("app_name",
                entities.getOrDefault("original_text", "")).trim().toLowerCase();

        if (appRaw.isBlank()) return "Which application would you like me to open?";

        // Strip common prefixes like "open", "launch", "start"
        appRaw = appRaw.replaceAll("(?i)^(open|launch|start|run)\\s+", "").trim();

        // Special case: YouTube → open in Chrome
        if (appRaw.contains("youtube")) {
            openProcess("open", "-a", "Google Chrome", "https://www.youtube.com");
            return "Opening YouTube in Chrome.";
        }

        // Look up alias map
        String appName = APP_ALIASES.get(appRaw);

        // No alias found — try the raw name directly (handles any installed app)
        if (appName == null) {
            appName = capitalize(appRaw);
        }

        boolean launched = openProcess("open", "-a", appName);
        if (launched) {
            return "Opening " + appName + ".";
        }

        // Last resort: open with system default handler
        openProcess("open", appRaw);
        return "Trying to open " + appName + ". If it doesn't open, check the app is installed.";
    }

    private boolean openProcess(String... cmd) {
        try {
            new ProcessBuilder(cmd).start();
            return true;
        } catch (Exception e) {
            log.warning("AppLaunch failed [" + String.join(" ", cmd) + "]: " + e.getMessage());
            return false;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
