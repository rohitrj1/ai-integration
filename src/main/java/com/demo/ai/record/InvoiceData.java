package com.demo.ai.record;

public record InvoiceData(
        String productName,
        int quantity,
        double margin,
        double totalAmount,
        String currency
) {}