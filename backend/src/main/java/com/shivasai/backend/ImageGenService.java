package com.shivasai.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageGenService {

    private static final int MAX_IMAGES = 6;

    private final VideoProjectRepository repository;
    private final VideoAssemblyService videoAssemblyService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Value("${pollinations.api.key}")
    private String apiKey;

    public ImageGenService(VideoProjectRepository repository, VideoAssemblyService videoAssemblyService) {
        this.repository = repository;
        this.videoAssemblyService = videoAssemblyService;
    }

    @Async
    public void generateImages(String projectId, String script) {
        VideoProject project = repository.findById(projectId).orElse(null);
        if (project == null) {
            System.out.println("Project not found: " + projectId);
            return;
        }

        try {
            List<String> prompts = splitScriptIntoPrompts(script);
            Path folder = Paths.get("storage", "images", projectId);
            Files.createDirectories(folder);

            List<String> savedPaths = new ArrayList<>();
            for (int i = 0; i < prompts.size(); i++) {
                Path file = folder.resolve("scene-" + (i + 1) + ".jpg");
                downloadImage(prompts.get(i), file);
                savedPaths.add(file.toString());
                System.out.println("Saved image " + (i + 1) + "/" + prompts.size());
                Thread.sleep(1500); // be gentle with the free API
            }

            project.setImageFilePaths(savedPaths);
            project.setStatus("IMAGES_READY");
            repository.save(project);
            System.out.println("All images ready for project " + projectId);
            videoAssemblyService.assembleVideo(projectId);

        } catch (Exception e) {
            System.out.println("Image generation failed: " + e.getMessage());
            project.setStatus("FAILED");
            repository.save(project);
        }
    }

    private List<String> splitScriptIntoPrompts(String script) {
        List<String> prompts = new ArrayList<>();
        for (String part : script.split("(?<=[.!?])\\s+|\\n+")) {
            String text = part.trim();
            if (text.length() > 10) {
                if (text.length() > 250) {
                    text = text.substring(0, 250);
                }
                prompts.add(text + ", cinematic, vertical, high quality, detailed");
            }
            if (prompts.size() == MAX_IMAGES) {
                break;
            }
        }
        if (prompts.isEmpty()) {
            throw new IllegalStateException("Script is empty, cannot create image prompts");
        }
        return prompts;
    }

    private void downloadImage(String prompt, Path target) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://gen.pollinations.ai/image/" + encoded
                + "?width=720&height=1280&nologo=true&seed=" + System.nanoTime() % 100000;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Pollinations returned status " + response.statusCode()
                    + ": " + new String(response.body(), StandardCharsets.UTF_8));
        }
        Files.write(target, response.body());
    }
}