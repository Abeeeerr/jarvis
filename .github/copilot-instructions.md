# Copilot / AI Agent Instructions for Jarvis

Purpose: give AI coding agents the concise, actionable context they need to be productive in this codebase.

Big picture
- This is a Spring Boot-based voice assistant application whose entry point is `src/main/java/com/jarvis/JarvisApplication.java`.
- Major components:
  - API layer: `src/main/java/com/jarvis/api/JarvisApiController.java` exposes REST endpoints used by the UI.
  - WebSocket: `src/main/java/com/jarvis/config/WebSocketConfig.java` configures realtime channels for the web client.
  - Voice flow controllers: `src/main/java/com/jarvis/controller/VoiceController.java` mediates audio/websocket events.
  - Skill system: `src/main/java/com/jarvis/skills` contains `Skill`, `SkillRegistry`, and many concrete skills (`greet`, `music`, `notes`, `reminder`, `chat`, etc.).
  - Skill dispatching & NLP: `src/main/java/com/jarvis/core/SkillDispatcher.java` routes `NlpResult` (see `nlp/NlpResult.java`) to skills. `IntentEngine.java`, `GemmaService.java`, and `EntityExtractor.java` implement intent detection and entity extraction.
  - STT / TTS: `src/main/java/com/jarvis/stt/VoskSTTService.java` and `src/main/java/com/jarvis/tts/TTSService.java` integrate speech recognition and synthesis.
  - Static UI & models: static frontend is in `src/main/resources/static` (index.html, app.js). Large speech-model assets live under `src/main/resources/model`.

Important code patterns & conventions
- Skills implement the `Skill` interface and are discovered/registered via `SkillRegistry` — follow existing skill classes (e.g., `greet/GreetSkill.java`) when adding new ones.
- `SkillDispatcher` receives an `NlpResult` and calls the best-matching `Skill`. Keep business logic in skill implementations, not the dispatcher.
- NLP pipeline returns `NlpResult` objects that include intent and entities — prefer using `IntentEngine` and `EntityExtractor` helpers rather than re-parsing raw text.
- STT/TTS are treated as services behind interfaces; new implementations should match existing method signatures to be pluggable.
- Keep large model files in `src/main/resources/model` and avoid duplicate copies — they are packaged into the JAR under `target/classes/model`.

Build / run / debug
- Build an executable jar: `mvn clean package` (artifact appears under `target/`).
- Run from Maven for iterative dev: `mvn spring-boot:run`.
- Run the packaged app: `java -jar target/jarvis-1.0.0-SNAPSHOT.jar` (or the produced filename).
- Debug in IDE: run `JarvisApplication` as a Spring Boot app. Websocket endpoints and REST controllers are available on the configured port in `src/main/resources/application.properties`.

Integration points & external dependencies
- External NLP: `GemmaService` wraps external NLP interactions — check network calls before changing behavior.
- Vosk STT: `VoskSTTService` relies on the model files under `resources/model/am`, `graph`, and `ivector`. Ensure model path availability when running outside the IDE.
- Static client communicates via REST and WebSocket endpoints defined in `JarvisApiController` and `WebSocketConfig`.

Examples to follow
- Adding a skill: copy `skills/greet/GreetSkill.java`, implement `Skill`, register via `SkillRegistry` pattern.
- Handling intents: create or extend `IntentEngine` patterns; produce `NlpResult` and let `SkillDispatcher` route it.

What to avoid / watch for
- Avoid modifying the `resources/model` layout; STT expects specific files/dirs (e.g., `am/final.mdl`, `graph/Gr.fst`).
- Do not move initialization logic out of `JarvisApplication` without verifying Spring Boot startup ordering.
- There are no unit tests in the repo — prefer small, isolated manual verification when changing runtime behavior.

If something is unclear
- Ask for specific runtime details (OS audio device setup, external NLP credentials) — those are not present in repo files.

Next steps for agents
- When implementing features, run `mvn spring-boot:run` and test the web UI at the app port; verify STT/TTS using small sample audio.

Feedback
- If any of the above assumptions are incorrect or you'd like examples expanded (e.g., typical `NlpResult` contents or Skill lifecycle), tell me which area to expand.
