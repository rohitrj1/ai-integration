package com.demo.ai.service;


import com.demo.ai.dto.InvoiceRequest;
import com.demo.ai.util.TallyXMLBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TallyService {

    private static final String TALLY_URL = "http://localhost:9000";

    private final RestTemplate restTemplate = new RestTemplate();


    public String pushData(InvoiceRequest request) {
        // 1. Create Vendor and capture response
        String vResp = createVendor(request);
        System.out.println("Vendor Creation Result: " + vResp);

        // Tally returns <CREATED>1</CREATED> or if it exists it might return <ALTERED>1</ALTERED>
        // Note: If Ledger exists, Tally sometimes returns <ERRORS>1</ERRORS> saying "Name already exists"
        if (vResp.contains("<CREATED>1</CREATED>") || vResp.contains("<ALTERED>1</ALTERED>") || vResp.contains("already exists")) {
            // 2. Only if Vendor is Ready, Create Invoice
            return createInvoice(request);
        } else {
            return "Bhai, Vendor nahi bana isliye Invoice skip kar diya. Response: " + vResp;
        }
    }

    // Change return type to String
    public String createVendor(InvoiceRequest request) {
        String xml = TallyXMLBuilder.vendorXML("Testing",
                request.getVendorId(),
                request.getVendorName()
        );
        return send(xml); // Return the response from send()
    }

    // Change return type to String
    public String createInvoice(InvoiceRequest request) {
        String xml = TallyXMLBuilder.invoiceXML("Testing",
                request.getVendorName(),
                request.getInvoiceAmount()
        );
        return send(xml); // Return the response from send()
    }

    private String send(String xml) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> entity = new HttpEntity<>(xml, headers);

            // Response ko capture karke print karein
            ResponseEntity<String> response = restTemplate.postForEntity(TALLY_URL, entity, String.class);

            System.out.println("Tally Result: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Connection Error: " + e.getMessage());
            return "ERROR";
        }
    }
}

