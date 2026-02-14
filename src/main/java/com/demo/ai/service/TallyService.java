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

    public void createVendor(InvoiceRequest request) {
        String xml = TallyXMLBuilder.vendorXML("Testing",
                request.getVendorId(),
                request.getVendorName()
        );
        send(xml);
    }

    public void createInvoice(InvoiceRequest request) {
        String xml = TallyXMLBuilder.invoiceXML("Testing",
                request.getVendorName(),
                request.getInvoiceAmount()
        );
        send(xml);
    }

    private void send(String xml) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        HttpEntity<String> entity = new HttpEntity<>(xml, headers);
        restTemplate.postForEntity(TALLY_URL, entity, String.class);
    }
}

