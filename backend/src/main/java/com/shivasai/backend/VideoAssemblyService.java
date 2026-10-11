package com.shivasai.backend;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class VideoAssemblyService {

    private final VideoProjectRepository repository;

    public VideoAssemblyService(VideoProjectRepository repository) {
        this.repository = repository;
    }

    @Async
    public void assembleVideo(String projectId) {
        VideoProject project = repository.findById(projectId).orElse(null);
        if (project == null) {
            System.out.println("Project not found: " + projectId);
            return;
        }

        try {
            project.setStatus("RENDERING");
            repository.save(project);

            List<String> images = project.getImageFilePaths();
            if (images == null || images.isEmpty()) {
                throw new IllegalStateException("No images to assemble");
            }

            Path audio = Paths.get(project.getAudioFilePath()).toAbsolutePath();
            double totalSeconds = getAudioDuration(audio);
            double secondsPerImage = totalSeconds / images.size();

            Path folder = Paths.get("storage", "videos").toAbsolutePath();
            Files.createDirectories(folder);

            StringBuilder list = new StringBuilder();
            String lastImage = "";
            for (String img : images) {
                lastImage = Paths.get(img).toAbsolutePath().toString().replace("\\", "/");
                list.append("file '").append(lastImage).append("'\n");
                list.append("duration ").append(String.format(Locale.US, "%.3f", secondsPerImage)).append("\n");
            }
            list.append("file '").append(lastImage).append("'\n"); // FFmpeg needs the last file repeated

            Path listFile = folder.resolve(projectId + "-list.txt");
            Files.writeString(listFile, list.toString(), StandardCharsets.UTF_8);

            Path output = folder.resolve(projectId + ".mp4");

            List<String> command = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-f", "concat", "-safe", "0", "-i", listFile.toString(),
                    "-i", audio.toString(),
                    "-vf", "scale=720:1280:force_original_aspect_ratio=increase,crop=720:1280,format=yuv420p",
                    "-r", "30",
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-shortest",
                    output.toString()
            ));
            runCommand(command);

            project.setFinalVideoPath(Paths.get("storage", "videos", projectId + ".mp4").toString());
            project.setStatus("DONE");
            repository.save(project);
            System.out.println("Video ready: " + project.getFinalVideoPath());

        } catch (Exception e) {
            System.out.println("Video assembly failed: " + e.getMessage());
            project.setStatus("FAILED");
            repository.save(project);
        }
    }

    private double getAudioDuration(Path audio) throws IOException, InterruptedException {
        String output = runCommand(List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audio.toString()
        ));
        return Double.parseDouble(output.trim());
    }

    private String runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String tail = output.length() > 600 ? output.substring(output.length() - 600) : output;
            throw new IOException(command.get(0) + " failed (code " + exitCode + "): " + tail);
        }
        return output;
    }
}