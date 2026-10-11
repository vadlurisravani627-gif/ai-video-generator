package com.shivasai.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173") // allows the React frontend to call this API
public class VideoProjectController {

    @Autowired
    private VideoProjectService service;

    @PostMapping
    public VideoProject createProject(@RequestBody Map<String, String> body) {
        String topic = body.get("topic");
        return service.createProject(topic);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoProject> getProject(@PathVariable String id) {
        return service.getProject(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<VideoProject> getAllProjects() {
        return service.getAllProjects();
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<Resource> getVideo(@PathVariable String id) {
        Optional<VideoProject> project = service.getProject(id);
        if (project.isEmpty() || project.get().getFinalVideoPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(project.get().getFinalVideoPath()).toAbsolutePath();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }
}