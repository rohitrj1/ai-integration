package com.demo.ai.controller;

import com.demo.ai.model.RefundResult;
import com.demo.ai.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    /**
     * POST /refund/process
     * multipart/form-data: mobile (text) + reason (text) + screenshot (file)
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RefundResult> process(
            @RequestParam("mobile")     String mobile,
            @RequestParam("reason")     String reason,
            @RequestParam("screenshot") MultipartFile screenshot) {

        if (mobile == null || mobile.isBlank())
            return ResponseEntity.badRequest().build();
        if (screenshot == null || screenshot.isEmpty())
            return ResponseEntity.badRequest().build();

        RefundResult result = refundService.processRefund(
                mobile.trim(), reason.trim(), screenshot);

        return ResponseEntity.ok(result);
    }
}