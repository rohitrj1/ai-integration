package com.demo.ai.util;

import com.demo.ai.model.ExtractedTxnInfo;
import com.demo.ai.model.FranchiseRequest;
import com.demo.ai.model.KioskReportRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EmailTemplates {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z").withZone(ZoneId.of("Asia/Kolkata"));

    private static final String BASE_STYLE =
            "body{font-family:Arial,sans-serif;margin:0;padding:20px;background:#f4f4f4}" +
                    ".card{background:#fff;border-radius:12px;max-width:560px;margin:0 auto;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.1)}" +
                    ".hdr{padding:22px 28px;color:#fff}" +
                    ".hdr h2{margin:0;font-size:19px}" +
                    ".hdr p{margin:4px 0 0;opacity:.8;font-size:12px}" +
                    ".body{padding:22px 28px}" +
                    ".badge{background:#fef3c7;color:#92400e;border-radius:20px;padding:3px 14px;font-size:12px;font-weight:700;display:inline-block;margin-bottom:14px}" +
                    "table{width:100%;border-collapse:collapse}" +
                    "td{padding:9px 10px;font-size:13.5px;border-bottom:1px solid #f0f0f0}" +
                    "td:first-child{color:#6b7280;width:38%}" +
                    "td:last-child{font-weight:600;color:#111}" +
                    ".note{background:#fef2f2;border-left:4px solid #ef4444;padding:12px 14px;border-radius:0 8px 8px 0;font-size:13px;color:#991b1b;margin-top:14px}" +
                    ".footer{background:#f9fafb;padding:12px 28px;font-size:11px;color:#9ca3af;text-align:center}";

    // ── SUPPORT — Refund alert ────────────────────────────────────────────────

    public static String supportRefund(
            String title, String mobile, String reason,
            String ticketId, ExtractedTxnInfo txn, String note) {

        String txnId  = val(txn != null ? txn.transactionId : null);
        String amount = txn != null && txn.amount != null ? "Rs." + txn.amount : "—";
        String method = val(txn != null ? txn.paymentMethod : null);
        String app    = val(txn != null ? txn.bankOrApp : null);
        String date   = val(txn != null ? txn.transactionDate : null);

        return html(BASE_STYLE,
                "<div class='card'>" +
                        "<div class='hdr' style='background:#15803d'>" +
                        "<h2>NutriTap Support — " + e(title) + "</h2>" +
                        "<p>Submitted: " + DT.format(Instant.now()) + "</p></div>" +
                        "<div class='body'>" +
                        "<div class='badge'>Ticket: " + e(ticketId) + "</div>" +
                        "<table>" +
                        row("Customer Mobile", mobile) +
                        row("Reason",          reason) +
                        row("Transaction ID",  txnId)  +
                        row("Amount",          amount) +
                        row("Payment Method",  method) +
                        row("App / Bank",      app)    +
                        row("Txn Date",        date)   +
                        "</table>" +
                        "<div class='note'><strong>Note:</strong> " + e(note) + "</div>" +
                        "</div>" +
                        "<div class='footer'>NutriTap AI Support Bot — Auto-generated</div>" +
                        "</div>");
    }

    // ── FRANCHISE enquiry ─────────────────────────────────────────────────────

    public static String franchise(FranchiseRequest r, String refId) {
        return html(
                BASE_STYLE + ".hdr{background:#1d4ed8}",
                "<div class='card'>" +
                        "<div class='hdr'><h2>New Franchise Enquiry</h2>" +
                        "<p>Ref: " + refId + " — " + DT.format(Instant.now()) + "</p></div>" +
                        "<div class='body'><table>" +
                        row("Name",          r.name)         +
                        row("Email",         r.email)        +
                        row("Phone",         r.phone)        +
                        row("City",          r.city)         +
                        row("Investment",    r.invest)       +
                        row("Location Type", r.locationType) +
                        row("Message",       r.message)      +
                        "</table></div>" +
                        "<div class='footer'>NutriTap Franchise Bot — Auto-generated</div>" +
                        "</div>");
    }

    // ── KIOSK issue report ────────────────────────────────────────────────────

    public static String kiosk(KioskReportRequest r, String ticketId) {
        return html(
                BASE_STYLE + ".hdr{background:#15803d}",
                "<div class='card'>" +
                        "<div class='hdr'><h2>Kiosk Issue Report</h2>" +
                        "<p>Ticket: " + ticketId + " — " + DT.format(Instant.now()) + "</p></div>" +
                        "<div class='body'><table>" +
                        row("Location",    r.location)  +
                        row("Machine ID",  r.machineId != null && !r.machineId.isBlank() ? r.machineId : "Not provided") +
                        row("Issue Type",  r.issueType) +
                        row("Description", r.description != null && !r.description.isBlank() ? r.description : "None") +
                        "</table></div>" +
                        "<div class='footer'>NutriTap Kiosk Bot — Assign technician within 4-6 hours</div>" +
                        "</div>");
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private static String html(String style, String body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>" +
                "<style>" + style + "</style></head><body>" + body + "</body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td>" + e(label) + "</td><td>" + e(val(value)) + "</td></tr>";
    }

    private static String e(String s) {
        if (s == null) return "—";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String val(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}