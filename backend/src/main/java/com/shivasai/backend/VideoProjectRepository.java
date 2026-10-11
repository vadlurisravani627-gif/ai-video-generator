
package com.shivasai.backend;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoProjectRepository extends MongoRepository<VideoProject, String> {
}