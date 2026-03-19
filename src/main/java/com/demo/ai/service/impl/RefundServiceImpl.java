package com.demo.ai.service.impl;

import com.demo.ai.model.ExtractedTxnInfo;
import com.demo.ai.model.RefundResult;
import com.demo.ai.service.EmailService;
import com.demo.ai.service.RefundService;
import com.demo.ai.service.WhatsAppService;
import com.demo.ai.util.EmailTemplates;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
public class RefundServiceImpl implements RefundService {

    @Autowired private EmailService           emailService;
    @Autowired private WhatsAppService whatsAppService;

    @Value("${nutritap.refund.api.url:https://api.nutritap.in/v1/refund}")
    private String refundApiUrl;

    @Value("${nutritap.refund.api.key:CHANGE_ME}")
    private String refundApiKey;

    private final RestTemplate rest   = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // ── MAIN PIPELINE ─────────────────────────────────────────────────────────

    @Override
    public RefundResult processRefund(String mobile, String reason, MultipartFile screenshot) {
        RefundResult result = new RefundResult();
        result.ticketId = "NT-REF-" + sixDigit();

        // Step 1: OCR screenshot via Gemini Vision
        ExtractedTxnInfo txn = extractTxnFromImage(screenshot);

        if (txn == null || txn.transactionId == null || txn.transactionId.isBlank()) {
            result.success     = false;
            result.needsManual = true;
            result.txnId       = "UNREADABLE";
            result.message     = "Could not read transaction ID from screenshot.";

            emailService.sendSupportEmail(
                    "[NutriTap] Refund - Manual Review Required | " + result.ticketId,
                    EmailTemplates.supportRefund("Manual Review Required", mobile, reason, result.ticketId, txn, "Screenshot unreadable"),
                    screenshot
            );
            whatsAppService.sendMessage(mobile,
                    "*NutriTap Refund Request Received* \u2705\n\n" +
                            "Ticket: *" + result.ticketId + "*\n" +
                            "Reason: " + reason + "\n" +
                            "Status: Under manual review\n\n" +
                            "Screenshot received via email. Our team will contact you within *24 hours*.\n\n" +
                            "- NutriTap Support");
            return result;
        }

        result.txnId  = txn.transactionId;
        result.amount = txn.amount;

        // Step 2: Call Refund API
        try {
            boolean apiSuccess = callRefundApi(txn.transactionId, mobile, reason, txn.amount);

            if (apiSuccess) {
                result.success  = true;
                result.timeline = resolveTimeline(txn.paymentMethod);
                result.message  = "Refund processed successfully.";

                emailService.sendSupportEmail(
                        "[NutriTap] Refund Auto-Processed | " + result.ticketId,
                        EmailTemplates.supportRefund("Auto-Processed", mobile, reason, result.ticketId, txn, "API approved"),
                        null
                );
                whatsAppService.sendMessage(mobile,
                        "Refund Initiated!\nTicket: " + result.ticketId +
                                "\nAmount: Rs." + (txn.amount != null ? txn.amount : "—") +
                                "\nExpected credit: " + result.timeline + "\n- NutriTap Support");
            } else {
                result.success     = false;
                result.needsManual = true;
                result.message     = "Refund API rejected. Escalated to support team.";

                emailService.sendSupportEmail(
                        "[NutriTap] Refund API Failed - Manual Action | " + result.ticketId,
                        EmailTemplates.supportRefund("API Rejected", mobile, reason, result.ticketId, txn, "API returned failure"),
                        screenshot
                );
                whatsAppService.sendMessage(mobile,
                        "*NutriTap Refund Update* ⚠️\n\n" +
                                "Ticket: *" + result.ticketId + "*\n" +
                                "Transaction ID: " + (txn != null ? txn.transactionId : "—") + "\n" +
                                "Status: Under manual review\n\n" +
                                "Our billing team will resolve within *24 hours*.\n" +
                                "- NutriTap Support");
            }
        } catch (Exception e) {
            result.success     = false;
            result.needsManual = true;
            result.message     = "Refund API unreachable. Escalated to support.";
            emailService.sendSupportEmail(
                    "[NutriTap] Refund API Unreachable - URGENT | " + result.ticketId,
                    EmailTemplates.supportRefund("API Unreachable - Urgent", mobile, reason, result.ticketId, txn, e.getMessage()),
                    screenshot
            );
        }
        return result;
    }

    // ── GEMINI VISION OCR ─────────────────────────────────────────────────────

    private ExtractedTxnInfo extractTxnFromImage(MultipartFile screenshot) {
        try {
            String b64      = Base64.getEncoder().encodeToString(screenshot.getBytes());
            String mimeType = screenshot.getContentType() != null ? screenshot.getContentType() : "image/jpeg";
            String prompt   = """
                You are a financial OCR assistant. Analyse this payment screenshot and return ONLY this JSON (no markdown):
                {
                  "transactionId": "UPI ref or order ID or payment reference (string or null)",
                  "amount": "numeric amount only e.g. 149 (string or null)",
                  "paymentMethod": "UPI or CREDIT_CARD or DEBIT_CARD or NET_BANKING or WALLET or UNKNOWN",
                  "merchantName": "merchant name or null",
                  "transactionDate": "DD/MM/YYYY or null",
                  "status": "SUCCESS or FAILED or PENDING",
                  "bankOrApp": "GPay or PhonePe or Paytm or HDFC etc or null"
                }
                """;

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("contents", List.of(Map.of("parts", List.of(
                    Map.of("text", prompt),
                    Map.of("inline_data", Map.of("mime_type", mimeType, "data", b64))
            ))));
            reqBody.put("generationConfig", Map.of("temperature", 0.0, "maxOutputTokens", 512));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String apiKey = System.getenv("GEMINI_API_KEY");
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            ResponseEntity<Map> resp = rest.postForEntity(url, new HttpEntity<>(reqBody, headers), Map.class);

            @SuppressWarnings("unchecked") Map<String, Object> body = (Map<String, Object>) resp.getBody();
            if (body == null) return null;

            @SuppressWarnings("unchecked") List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            @SuppressWarnings("unchecked") Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked") List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String raw = (String) parts.get(0).get("text");

            String clean = raw.trim()
                    .replaceAll("(?s)^```json\\s*", "").replaceAll("(?s)^```\\s*", "").replaceAll("(?s)\\s*```$", "").trim();

            JsonNode node = mapper.readTree(clean);
            ExtractedTxnInfo info = new ExtractedTxnInfo();
            info.transactionId   = textOrNull(node, "transactionId");
            info.amount          = textOrNull(node, "amount");
            info.paymentMethod   = textOrNull(node, "paymentMethod");
            info.merchantName    = textOrNull(node, "merchantName");
            info.transactionDate = textOrNull(node, "transactionDate");
            info.status          = textOrNull(node, "status");
            info.bankOrApp       = textOrNull(node, "bankOrApp");

            System.out.printf("[Refund] OCR: txnId=%s amount=%s method=%s%n",
                    info.transactionId, info.amount, info.paymentMethod);
            return info;

        } catch (Exception e) {
            System.err.println("[Refund] OCR error: " + e.getMessage());
            return null;
        }
    }

    // ── REFUND API CALL ───────────────────────────────────────────────────────

    private boolean callRefundApi(String txnId, String mobile, String reason, String amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transactionId",   txnId);
        payload.put("customerMobile",  mobile);
        payload.put("reason",          reason);
        payload.put("amount",          amount);
        payload.put("source",          "NutriTap-SupportBot");
        payload.put("timestamp",       Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key",    refundApiKey);
        headers.set("X-Request-Id", "NT-" + sixDigit());

        ResponseEntity<Map> resp = rest.postForEntity(
                refundApiUrl, new HttpEntity<>(payload, headers), Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();

        if (resp.getStatusCode().is2xxSuccessful() && body != null) {
            return Boolean.TRUE.equals(body.get("success")) || "APPROVED".equals(body.get("status"));
        }
        return false;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private String resolveTimeline(String method) {
        if (method == null) return "3-5 business days";
        return switch (method.toUpperCase()) {
            case "UPI"         -> "24-48 hours";
            case "CREDIT_CARD" -> "5-7 business days";
            case "DEBIT_CARD"  -> "3-5 business days";
            case "NET_BANKING" -> "5-7 business days";
            case "WALLET"      -> "24 hours";
            default            -> "3-5 business days";
        };
    }

    private String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || v.asText().equalsIgnoreCase("null")) return null;
        return v.asText().trim();
    }


private String sixDigit() { return String.valueOf(100_000 + new Random().nextInt(900_000)); }
}