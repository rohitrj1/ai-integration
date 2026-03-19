package com.demo.ai.service.impl;

import com.demo.ai.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply}")
    private String fromEmail;

    @Value("${nutritap.from.name:NutriTap Support}")
    private String fromName;

    @Value("${nutritap.support.email:support}")
    private String supportEmail;

    /**
     * Sends HTML email to support team with screenshot attached.
     *
     * ROOT CAUSE FIX: MultipartFile is tied to the HTTP request stream.
     * If we pass it directly to an @Async method, the request closes before
     * the async thread reads the bytes — resulting in an empty attachment.
     *
     * FIX: Read bytes EAGERLY here (on the request thread), then pass
     * the raw byte[] to the async sender.
     */
    @Override
    public void sendSupportEmail(String subject, String htmlBody, MultipartFile attachment) {
        try {
            // ── Read bytes NOW on the request thread ──────────────────────────
            byte[] fileBytes    = (attachment != null && !attachment.isEmpty()) ? attachment.getBytes() : null;
            String fileName     = attachment != null ? attachment.getOriginalFilename() : null;
            String contentType  = (attachment != null && attachment.getContentType() != null)
                    ? attachment.getContentType() : "image/jpeg";

            // ── Hand off to async thread with raw bytes ────────────────────────
            sendSupportEmailAsync(subject, htmlBody, fileBytes, fileName, contentType);

        } catch (Exception e) {
            System.err.println("[Email] Failed to read attachment bytes: " + e.getMessage());
        }
    }

    @Async
    public void sendSupportEmailAsync(
            String subject, String htmlBody,
            byte[] fileBytes, String fileName, String contentType) {
        try {
            MimeMessage msg     = mailSender.createMimeMessage();
            // true = multipart MIME — required for attachments
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");

            h.setFrom(fromEmail, fromName);
            h.setTo(supportEmail);
            h.setSubject(subject);
            h.setText(htmlBody, true); // true = isHtml

            // Attach screenshot if bytes were read successfully
            if (fileBytes != null && fileBytes.length > 0) {
                String ext            = getExt(fileName);
                String attachmentName = "transaction-screenshot." + ext;

                h.addAttachment(
                        attachmentName,
                        new ByteArrayResource(fileBytes),   // in-memory, no temp file
                        contentType
                );
                System.out.println("[Email] Attaching screenshot: " + attachmentName
                        + " (" + fileBytes.length + " bytes)");
            } else {
                System.out.println("[Email] No attachment — sending email without screenshot.");
            }

            mailSender.send(msg);
            System.out.println("[Email] Support email sent -> " + supportEmail + " | " + subject);

        } catch (Exception e) {
            System.err.println("[Email] Support send FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plain HTML email — no attachment needed (franchise, kiosk alerts).
     */
    @Async
    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg     = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(htmlBody, true);
            mailSender.send(msg);
            System.out.println("[Email] Sent -> " + to + " | " + subject);

        } catch (Exception e) {
            System.err.println("[Email] Send FAILED -> " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}