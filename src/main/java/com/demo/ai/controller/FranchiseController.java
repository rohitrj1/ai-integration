package com.demo.ai.controller;

import com.demo.ai.model.FranchiseRequest;
import com.demo.ai.service.FranchiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/franchise")
public class FranchiseController {

    @Autowired
    private FranchiseService franchiseService;

    /**
     * POST /franchise/enquiry
     * Body: { name, email, phone, city, invest, locationType, message }
     */
    @PostMapping("/enquiry")
    public ResponseEntity<Map<String, String>> enquiry(@RequestBody FranchiseRequest request) {
        if (request.name  == null || request.name.isBlank() ||
                request.email == null || request.email.isBlank() ||
                request.city  == null || request.city.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name, email and city are required."));
        }
        String refId = franchiseService.submitEnquiry(request);
        return ResponseEntity.ok(Map.of("refId", refId, "status", "received"));
    }
}