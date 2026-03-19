package com.demo.ai.service.impl;

import com.demo.ai.model.KioskReportRequest;
import com.demo.ai.service.EmailService;
import com.demo.ai.service.KioskService;
import com.demo.ai.util.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class KioskServiceImpl implements KioskService {

    @Autowired private EmailService emailService;

    @Value("${nutritap.support.email:support}")
    private String supportEmail;

    @Override
    public String submitReport(KioskReportRequest req) {
        String ticketId = "NT-KSK-" + sixDigit();
        emailService.sendEmail(supportEmail,
                "[NutriTap] Kiosk Issue - " + req.location + " | " + ticketId,
                EmailTemplates.kiosk(req, ticketId));
        return ticketId;
    }

    private String sixDigit() { return String.valueOf(100_000 + new Random().nextInt(900_000)); }
}