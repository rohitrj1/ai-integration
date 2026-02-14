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

        tallyService.createVendor(request);
        tallyService.createInvoice(request);

        return ResponseEntity.ok("Vendor and Invoice stored in Tally");
    }
}