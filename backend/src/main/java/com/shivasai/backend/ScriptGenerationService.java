package com.shivasai.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ScriptGenerationService {

    @Autowired
    private VideoProjectRepository repository;

    @Autowired
    private TtsService ttsService;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestClient restClient = RestClient.create();

    @Async
    public void generateScript(String projectId, String topic) {
        try {
            String prompt = "Write a short, engaging narration script (100-150 words) for a short video about: "
                    + topic + ". Write only the narration text, no scene directions, no titles, no markdown.";

            Map<String, Object> requestBody = Map.of(
                "model", "openai/gpt-oss-120b",
         
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            Map<?, ?> response = restClient.post()
                    .uri("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            String script = (String) message.get("content");

            repository.findById(projectId).ifPresent(project -> {
                project.setScript(script);
                project.setStatus("SCRIPT_READY");
                repository.save(project);
            });

            ttsService.generateAudio(projectId, script);

        } catch (Exception e) {
            e.printStackTrace();
            repository.findById(projectId).ifPresent(project -> {
                project.setStatus("FAILED");
                repository.save(project);
            });
        }
    }
}