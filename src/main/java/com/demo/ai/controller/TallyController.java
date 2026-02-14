package com.demo.ai.controller;

import com.demo.ai.dto.InvoiceRequest;
import com.demo.ai.service.TallyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tally")
public class TallyController {

    private final TallyService tallyService;

    public TallyController(TallyService tallyService) {
        this.tallyService = tallyService;
    }

    @PostMapping("/push-invoice")
    public ResponseEntity<String> pushInvoice(@RequestBody InvoiceRequest request) {
        String finalResult = tallyService.pushData(request);

        if(finalResult.contains("<CREATED>1</CREATED>")) {
            return ResponseEntity.ok("Success: Data Synced to Tally!");
        } else {
            return ResponseEntity.status(500).body("Tally Response: " + finalResult);
        }
    }
}