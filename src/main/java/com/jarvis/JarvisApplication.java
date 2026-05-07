package com.jarvis;

import com.jarvis.controller.VoiceController;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JarvisApplication {

    public static void main(String[] args) {
        SpringApplication.run(JarvisApplication.class, args);
    }

    /**
     * Starts the microphone listen loop on a daemon thread so the web server
     * (port 8080) stays responsive at the same time.
     */
    @Bean
    public CommandLineRunner startAssistant(VoiceController voiceController) {
        return args -> {
            System.out.println("===========================================");
            System.out.println("  Jarvis Voice Assistant  —  Starting up  ");
            System.out.println("  Web UI: http://localhost:8080            ");
            System.out.println("===========================================");

            Thread listenThread = new Thread(voiceController::run, "jarvis-listen");
            listenThread.setDaemon(true);
            listenThread.start();
        };
    }
}
