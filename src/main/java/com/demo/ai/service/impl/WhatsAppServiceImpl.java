package com.demo.ai.service.impl;

import com.demo.ai.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${twilio.account.sid:}")
    private String twilioSid;

    @Value("${twilio.auth.token:}")
    private String twilioToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String twilioFrom;

    private final RestTemplate rest = new RestTemplate();

    @Async
    @Override
    public void sendMessage(String mobile, String message) {
        sendToTwilio(mobile, message, null);
    }

    /**
     * Sends WhatsApp message WITH an image attachment.
     *
     * mediaUrl must be a publicly accessible URL (https://).
     * Twilio fetches the image from this URL and attaches it to WhatsApp.
     *
     * SANDBOX NOTE: Sandbox supports MediaUrl — image will appear in WhatsApp.
     * PRODUCTION: Works the same way.
     *
     * Options for hosting the screenshot URL:
     *  1. Upload to AWS S3 / Cloudinary / Firebase → use that URL   (recommended)
     *  2. Expose via your own server endpoint /screenshots/{id}      (see below)
     *  3. Use ngrok during local testing to expose localhost
     */
    @Async
    @Override
    public void sendMessageWithMedia(String mobile, String message, String mediaUrl) {
        sendToTwilio(mobile, message, mediaUrl);
    }

    // ── CORE SENDER ──────────────────────────────────────────────────────────

    private void sendToTwilio(String mobile, String message, String mediaUrl) {
        if (twilioSid == null || twilioSid.isBlank()) {
            System.out.println("[WhatsApp] Skipped — Twilio not configured.");
            return;
        }
        try {
            String digits   = mobile.replaceAll("[^0-9]", "");
            String toNumber = digits.length() == 10 ? "+91" + digits : "+" + digits;
            String toWa     = "whatsapp:" + toNumber;

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json";

            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", twilioFrom);
            form.add("To",   toWa);
            form.add("Body", message);

            // Attach image if URL provided
            if (mediaUrl != null && !mediaUrl.isBlank()) {
                form.add("MediaUrl", mediaUrl);
                System.out.println("[WhatsApp] Attaching media: " + mediaUrl);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String creds = Base64.getEncoder().encodeToString((twilioSid + ":" + twilioToken).getBytes());
            headers.set("Authorization", "Basic " + creds);

            ResponseEntity<Map> resp = rest.postForEntity(
                    url, new HttpEntity<>(form, headers), Map.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                System.out.println("[WhatsApp] Sent -> " + toWa + (mediaUrl != null ? " + image" : ""));
            } else {
                System.err.println("[WhatsApp] Failed: HTTP " + resp.getStatusCode().value());
            }
        } catch (Exception e) {
            System.err.println("[WhatsApp] FAILED: " + e.getMessage());
        }
    }
}