package com.demo.ai.service.impl;

import com.demo.ai.model.FranchiseRequest;
import com.demo.ai.service.EmailService;
import com.demo.ai.service.FranchiseService;
import com.demo.ai.service.WhatsAppService;
import com.demo.ai.util.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class FranchiseServiceImpl implements FranchiseService {

    @Autowired private EmailService    emailService;
    @Autowired private WhatsAppService whatsAppService;

    @Value("${nutritap.franchise.email:franchise}")
    private String franchiseEmail;

    @Override
    public String submitEnquiry(FranchiseRequest req) {
        String refId = "NT-FRAN-" + sixDigit();

        emailService.sendEmail(franchiseEmail,
                "[NutriTap] New Franchise Enquiry - " + req.city + " | " + refId,
                EmailTemplates.franchise(req, refId));

        if (req.phone != null && !req.phone.isBlank()) {
            whatsAppService.sendMessage(req.phone,
                    "Namaste " + req.name + " Ji!\n\n" +
                            "Thank you for your interest in NutriTap Franchise!\n" +
                            "Reference ID: " + refId + "\n" +
                            "City: " + req.city + "\n\n" +
                            "Our franchise team will call you within 48 hours.\n- Team NutriTap");
        }
        return refId;
    }

    private String sixDigit() { return String.valueOf(100_000 + new Random().nextInt(900_000)); }
}