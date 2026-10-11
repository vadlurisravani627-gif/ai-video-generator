package com.shivasai.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VideoProjectService {

    @Autowired
    private VideoProjectRepository repository;

    @Autowired
    private ScriptGenerationService scriptGenerationService;

    public VideoProject createProject(String topic) {
        VideoProject project = new VideoProject();
        project.setTopic(topic);
        VideoProject saved = repository.save(project);

        scriptGenerationService.generateScript(saved.getId(), topic);

        return saved;
    }

    public Optional<VideoProject> getProject(String id) {
        return repository.findById(id);
    }

    public List<VideoProject> getAllProjects() {
        return repository.findAll();
    }
}