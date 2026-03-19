package com.demo.ai.controller;

import com.demo.ai.service.AIService;
import com.demo.ai.service.impl.AIServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
public class ChatController {

    @Autowired private AIService     aiService;
    @Autowired private AIServiceImpl aiServiceImpl;

    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam String q,
            @RequestParam(defaultValue = "default") String sessionId,
            HttpSession session) {

        if (q == null || q.isBlank())
            return ResponseEntity.badRequest().body("Query cannot be empty.");

        String sid      = "default".equals(sessionId) ? session.getId() : sessionId;
        String sanitized = q.length() > 800 ? q.substring(0, 800) : q.trim();
        return ResponseEntity.ok(aiServiceImpl.getChatResponse(sanitized, sid));
    }

    @DeleteMapping("/session/clear")
    public ResponseEntity<String> clearSession(HttpSession session) {
        aiServiceImpl.clearSession(session.getId());
        session.invalidate();
        return ResponseEntity.ok("Session cleared.");
    }
}