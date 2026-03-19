package com.demo.ai.controller;

import com.demo.ai.service.impl.AIServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AIServiceImpl aiServiceImpl;

    /**
     * POST /admin/refresh
     * Manually trigger knowledge base re-crawl.
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        new Thread(aiServiceImpl::refreshBotMemory, "manual-refresh").start();
        return ResponseEntity.ok("Knowledge refresh started in background.");
    }

    /**
     * GET /admin/status
     * Bot health: last refresh time, pages crawled, active sessions.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(aiServiceImpl.getStatus());
    }
}