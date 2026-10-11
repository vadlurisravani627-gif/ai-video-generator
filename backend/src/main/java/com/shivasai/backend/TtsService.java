package com.shivasai.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TtsService {

    @Autowired
    private VideoProjectRepository repository;

    @Autowired
    private ImageGenService imageGenService;

    private static final String AUDIO_DIR = "storage/audio";

    @Async
    public void generateAudio(String projectId, String script) {
        try {
            Files.createDirectories(Paths.get(AUDIO_DIR));

            String fileName = projectId + ".mp3";
            String outputPath = AUDIO_DIR + File.separator + fileName;

            ProcessBuilder pb = new ProcessBuilder(
                    "edge-tts",
                    "--voice", "en-US-AriaNeural",
                    "--text", script,
                    "--write-media", outputPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output so the process doesn't hang on a full buffer
            process.getInputStream().readAllBytes();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                repository.findById(projectId).ifPresent(project -> {
                    project.setAudioFilePath(outputPath);
                    project.setStatus("AUDIO_READY");
                    repository.save(project);
                });

                // Kick off image generation next
                repository.findById(projectId).ifPresent(project ->
                        imageGenService.generateImages(projectId, project.getScript())
                );
            } else {
                throw new RuntimeException("edge-tts exited with code " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            repository.findById(projectId).ifPresent(project -> {
                project.setStatus("FAILED");
                repository.save(project);
            });
        }
    }
}