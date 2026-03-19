package com.demo.ai.service;

import org.springframework.web.multipart.MultipartFile;

public interface EmailService {
    // Reads bytes eagerly on request thread, then fires async send with attachment
    void sendSupportEmail(String subject, String htmlBody, MultipartFile attachment);
    void sendEmail(String to, String subject, String htmlBody);
}