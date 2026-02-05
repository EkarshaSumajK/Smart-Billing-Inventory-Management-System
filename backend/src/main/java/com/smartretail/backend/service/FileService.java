package com.smartretail.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

public interface FileService {
    String uploadImage(MultipartFile file, String fileName) throws IOException;
    Map<String, Object> getImage(String imageId);
}