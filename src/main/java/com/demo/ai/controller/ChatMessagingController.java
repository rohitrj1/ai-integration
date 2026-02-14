package com.demo.ai.controller;

import com.demo.ai.record.InvoiceData;
import com.demo.ai.record.InvoiceResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/")
public class ChatMessagingController {

    private final ChatClient chatClient;

    // Spring Boot 4.x automatically provides the ChatClient.Builder
    public ChatMessagingController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a helpful assistant for the AiBackend application.")
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<String> sendMessage(@RequestParam(value = "q") String q) {
        try {
            String result = chatClient.prompt().user(q).call().content();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Detailed logging
            System.err.println("AI Error: " + e.getMessage());

            if (e.getMessage().contains("429") || e.getMessage().contains("Quota")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Google says: Quota Limit Hit. Try again in a few seconds, or switch to 'gemini-1.5-flash-lite'.");
            }
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    @PostMapping(value = "extract-invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InvoiceData extractInvoice(@RequestParam("file") MultipartFile file) {
        try {
            // Resolve MimeType String
            String mimeTypeStr = file.getContentType();
            if (mimeTypeStr == null || mimeTypeStr.equals("application/octet-stream")) {
                mimeTypeStr = MimeTypeUtils.IMAGE_JPEG_VALUE; // Default to JPEG
            }

            // Final variable for lambda
            String finalMimeType = mimeTypeStr;

            return chatClient.prompt()
                    .user(u -> u
                            .text("Extract product name, quantity, margin, and total amount from this document.")
                            // Use MimeTypeUtils to wrap the string
                            .media(MimeTypeUtils.parseMimeType(finalMimeType), file.getResource())
                    )
                    .call()
                    .entity(InvoiceData.class);

        } catch (Exception e) {
            throw new RuntimeException("Extraction Failed: " + e.getMessage());
        }
    }


    @PostMapping(value = "extract-invoice-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InvoiceResponse extractItems(@RequestParam("file") MultipartFile file) {

        String mimeTypeStr = file.getContentType();
        if (mimeTypeStr == null || mimeTypeStr.equals("application/octet-stream")) {
            mimeTypeStr = MimeTypeUtils.IMAGE_JPEG_VALUE; // Default to JPEG
        }

        // Final variable for lambda
        String finalMimeType = mimeTypeStr;

        return chatClient.prompt()
                .system("""
            You are a professional accountant. Extract all line items from the provided document.
            - If a 'Rate' or 'Price' is mentioned, put it in unitRate.
            - If 'Margin' is mentioned, put it in margin.
            - If 'Rate' is missing but 'Total' and 'Quantity' are there, calculate the rate.
            - Return a list of all items found.
            """)
                .user(u -> u
                        .text("Extract the item-wise data from this invoice.")
                        .media(MimeTypeUtils.parseMimeType(finalMimeType), file.getResource())
                )
                .call()
                .entity(InvoiceResponse.class); // Spring AI handles the List mapping automatically
    }

}
