package com.demo.ai.controller;

import com.demo.ai.model.KioskReportRequest;
import com.demo.ai.service.KioskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/kiosk")
public class KioskController {

    @Autowired
    private KioskService kioskService;

    /**
     * POST /kiosk/report
     * Body: { location, machineId, issueType, description }
     */
    @PostMapping("/report")
    public ResponseEntity<Map<String, String>> report(@RequestBody KioskReportRequest request) {
        if (request.location  == null || request.location.isBlank() ||
                request.issueType == null || request.issueType.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "location and issueType are required."));
        }
        String ticketId = kioskService.submitReport(request);
        return ResponseEntity.ok(Map.of("ticketId", ticketId, "status", "reported"));
    }
}