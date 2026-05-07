package com.jarvis.stt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sound.sampled.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * VoskSTTService — wraps the Vosk offline speech-recognition library.
 *
 * HOW TO GET A MODEL
 * ------------------
 * 1. Visit https://alphacephei.com/vosk/models
 * 2. Download a small English model, e.g. "vosk-model-small-en-us-0.15"
 * 3. Unzip it so that the directory structure looks like:
 *
 *    src/main/resources/model/
 *        am/
 *        conf/
 *        graph/
 *        ivector/
 *
 * 4. Set the path in application.properties:
 *        jarvis.model.path=src/main/resources/model
 *
 * Audio format requirements (must match Vosk model):
 *   • Sample rate  : 16 000 Hz
 *   • Bit depth    : 16-bit signed
 *   • Channels     : 1 (mono)
 *   • Byte order   : little-endian
 */
@Service
public class VoskSTTService {

    private static final Logger log = Logger.getLogger(VoskSTTService.class.getName());

    /** Sample rate expected by Vosk models (Hz). */
    private static final float SAMPLE_RATE = 16_000f;

    /** Number of audio bytes to read per microphone poll. */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Silence threshold: how many consecutive silent buffers constitute the
     * end of an utterance.  At 16 kHz / 16-bit the buffer holds 2 048 samples,
     * so 20 buffers ≈ ~2.5 seconds of silence.
     */
    private static final int SILENCE_THRESHOLD_BUFFERS = 20;

    @Value("${jarvis.model.path:src/main/resources/model}")
    private String modelPath;

    private Model model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
        try {
            log.info("Loading Vosk model from: " + modelPath);
            model = new Model(modelPath);
            log.info("Vosk model loaded successfully.");
        } catch (Throwable e) {
            // Catches both IOException and native UnsatisfiedLinkError (JNA / ARM64)
            log.log(Level.SEVERE,
                    "Vosk STT unavailable (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). "
                    + "Voice input will be disabled; text input via the web UI still works.", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
            log.info("Vosk model closed.");
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Opens the default microphone, captures audio until silence is detected,
     * and returns the recognised text.
     *
     * @return lower-cased recognised text, or an empty string on failure.
     */
    public String startListening() {
        if (model == null) {
            log.warning("Vosk model is not loaded — cannot listen.");
            return "";
        }

        AudioFormat format = buildAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            log.severe("Microphone line is not supported on this system.");
            return "";
        }

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
             Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {

            microphone.open(format);
            microphone.start();

            log.info("Microphone open — listening for speech…");

            byte[] buffer = new byte[BUFFER_SIZE];
            int silentBuffers = 0;
            boolean speechStarted = false;

            while (true) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }

                boolean isSilent = isSilent(buffer, bytesRead);

                if (!speechStarted && isSilent) {
                    // Still waiting for the user to start speaking — keep looping.
                    continue;
                }

                if (!isSilent) {
                    speechStarted = true;
                    silentBuffers = 0;
                }

                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    // Full result available — utterance likely complete.
                    return extractText(recognizer.getResult());
                }

                if (speechStarted && isSilent) {
                    silentBuffers++;
                    if (silentBuffers >= SILENCE_THRESHOLD_BUFFERS) {
                        // Enough silence — treat the utterance as done.
                        return extractText(recognizer.getFinalResult());
                    }
                }
            }

        } catch (Throwable e) {
            log.log(Level.SEVERE, "Error during speech recognition: " + e.getMessage(), e);
        }

        return "";
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the {@link AudioFormat} that Vosk models expect.
     */
    private AudioFormat buildAudioFormat() {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,   // sample rate
                16,            // sample size in bits
                1,             // channels (mono)
                2,             // frame size in bytes (16-bit mono = 2)
                SAMPLE_RATE,   // frame rate
                false          // little-endian
        );
    }

    /**
     * Extracts the {@code "text"} field from a Vosk JSON result.
     * Returns an empty string if the field is missing or blank.
     */
    private String extractText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode textNode = root.get("text");
            if (textNode != null && !textNode.asText().isBlank()) {
                return textNode.asText().trim().toLowerCase();
            }
        } catch (IOException e) {
            log.warning("Could not parse Vosk result JSON: " + json);
        }
        return "";
    }

    /**
     * Heuristic silence detection: considers a buffer silent when its
     * root-mean-square amplitude is below a low threshold.
     */
    private boolean isSilent(byte[] buffer, int length) {
        long sumOfSquares = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            // Reconstruct 16-bit little-endian sample.
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sumOfSquares += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sumOfSquares / (length / 2));
        return rms < 500; // Tune this threshold for your microphone environment.
    }
}
