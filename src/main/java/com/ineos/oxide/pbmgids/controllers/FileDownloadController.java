package com.ineos.oxide.pbmgids.controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileDownloadController {

    @GetMapping(value = "/static/**")
    public ResponseEntity<Resource> downloadFile(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Extract the file path from the request
            String requestPath = request.getRequestURI();
            String filePath = requestPath.substring("/static/".length());

            // Try to find the file in the file system first
            Path fsPath = Paths.get("./static/" + filePath);
            Resource resource;

            if (Files.exists(fsPath)) {
                resource = new FileSystemResource(fsPath);
            } else {
                // Fall back to classpath resource
                resource = new ClassPathResource("static/" + filePath);
            }

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(Paths.get(resource.getFilename()));
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            } catch (Exception e) {
                // Use default content type if detection fails
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}