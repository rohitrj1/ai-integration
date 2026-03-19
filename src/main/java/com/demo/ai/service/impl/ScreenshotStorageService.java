package com.demo.ai.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Saves screenshot to local disk and returns a public URL.
 * Twilio fetches this URL to attach the image to WhatsApp.
 *
 * IMPORTANT: Your server must be publicly accessible for Twilio to fetch it.
 * - Local dev: use ngrok  → ngrok http 8082  → set nutritap.app.url=https://xxxx.ngrok.io
 * - Production: set nutritap.app.url=https://yourdomain.com
 *
 * Files are stored in /tmp/nutritap-screenshots/ and auto-served via /screenshots/{filename}
 * See ScreenshotController for the serving endpoint.
 */
@Service
public class ScreenshotStorageService {

    @Value("${nutritap.app.url:http://localhost:8082}")
    private String appUrl;

    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir")
            + File.separator + "nutritap-screenshots" + File.separator;

    /**
     * Saves file to disk, returns full public URL.
     * e.g. https://yourdomain.com/screenshots/abc123.jpg
     */
    public String save(MultipartFile file) throws IOException {
        // Create directory if missing
        Path dir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        // Unique filename
        String ext      = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path   target   = dir.resolve(filename);

        file.transferTo(target.toFile());
        System.out.println("[Screenshot] Saved: " + target);

        return appUrl.stripTrailing() + "/screenshots/" + filename;
    }

    /**
     * Deletes file after it's been sent (optional cleanup).
     */
    public void delete(String filename) {
        try {
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            System.err.println("[Screenshot] Delete failed: " + e.getMessage());
        }
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}