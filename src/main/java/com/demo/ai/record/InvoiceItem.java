package com.demo.ai.record;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record InvoiceItem(
        @JsonPropertyDescription("The name or description of the product")
        String productName,

        @JsonPropertyDescription("Quantity of the item. Use 1 if not specified.")
        int quantity,

        @JsonPropertyDescription("The unit price or rate per item")
        Double unitRate,

        @JsonPropertyDescription("The margin or markup percentage if mentioned")
        Double margin,

        @JsonPropertyDescription("The total amount for this specific line item")
        double totalAmount
) {}
