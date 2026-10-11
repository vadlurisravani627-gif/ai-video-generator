package com.shivasai.backend;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "video_projects")
public class VideoProject {

    @Id
    private String id;

    private String topic;
    private String script;
    private String status = "PENDING"; // PENDING, SCRIPT_READY, AUDIO_READY, IMAGES_READY, RENDERING, DONE, FAILED

    private String audioFilePath;
    private List<String> imageFilePaths = new ArrayList<>();
    private String finalVideoPath;

    private LocalDateTime createdAt = LocalDateTime.now();
}