package com.demo.ai.controller;

import org.jsoup.Jsoup;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


@RestController
public class AIChatBootController {

    private final ChatClient chatClient;

    public AIChatBootController(ChatClient.Builder builder) {

        String websiteData;
        try {
            // JSoup se website ka text nikalna (Vastly Secure & Fast)
            websiteData = Jsoup.connect("https://nutritap.in/").get().text();
        } catch (Exception e) {
            websiteData = "NutriTap is India's leading smart retail and vending provider.";
        }

        this.chatClient = builder
                .defaultSystem("You are the NutriTap Support Bot. Use ONLY this data: " + websiteData +
                        ". If the answer is not there, say you only know about NutriTap.")
                .build();
    }

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index"); // Ye 'templates/index.html' ko dhoondega
    }

    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<String> sendMessage(@RequestParam(value = "q") String q) {
        try {
            String content = chatClient.prompt().user(q).call().content();
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
