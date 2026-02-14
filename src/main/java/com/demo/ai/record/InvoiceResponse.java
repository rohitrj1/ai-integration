package com.demo.ai.record;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record InvoiceResponse(
        String invoiceNumber,
        List<InvoiceItem> items,
        double grandTotal
) {}

