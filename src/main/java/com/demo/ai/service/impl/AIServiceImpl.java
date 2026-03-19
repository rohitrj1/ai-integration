package com.demo.ai.service.impl;

import com.demo.ai.service.AIService;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private ChatClient.Builder builder;

    private volatile ChatClient chatClient;
    private volatile String     knowledgeBase = "";
    private volatile Instant    lastRefreshed = null;
    private final AtomicBoolean refreshing    = new AtomicBoolean(false);

    private final String baseUrl = "https://nutritap.in";
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nt-refresh");
        t.setDaemon(true);
        return t;
    });

    private static final String BASE_SYSTEM = """
        IDENTITY: You are the NutriTap Support Bot — helpful, friendly AI for NutriTap,
        India's smart healthy vending kiosk brand.
        PERSONA: Warm, concise. Simple English + occasional Hindi (Namaste, Ji).
        Never mention OpenAI, Google, Gemini, or any AI model.
        RULES:
        - Max 100 words unless deep explanation needed.
        - Bullet points for multi-step answers.
        - For refunds: collect Order/Txn ID + registered mobile.
        - Always suggest a next step.
        REFUND POLICY: UPI 24-48h, Debit/Credit card 3-5 days, Net Banking 5-7 days.
        KIOSK: For dispense failures, assure 24-hr auto-refund, raise ticket.
        FRANCHISE: Investment from Rs.2L for 1 kiosk. ROI 18-24 months.
        ESCALATION: If user frustrated (2+ complaints) offer live agent.
        BOUNDARY: Only NutriTap queries. Off-topic politely decline.
        """;

    private static final String REFUND_CTX    = "[CTX:REFUND] User has payment issue. Be empathetic. Get Txn ID + mobile. State refund timeline.";
    private static final String KIOSK_CTX     = "[CTX:KIOSK] User has machine issue. Get location + machine ID. Assure 24-hr refund if money deducted.";
    private static final String FRANCHISE_CTX = "[CTX:FRANCHISE] User wants to partner. Highlight ROI, locations, support. Collect name/email/city/investment.";

    // ── INIT ──────────────────────────────────────────────────────────────────

    public AIServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(BASE_SYSTEM + "\n\nProduct Data: [Loading...]")
                .build();
    }

    @PostConstruct
    public void init() {
        scheduler.execute(this::refreshBotMemory);
        scheduler.scheduleAtFixedRate(this::refreshBotMemory, 6, 6, TimeUnit.HOURS);
    }

    // ── MEMORY REFRESH ────────────────────────────────────────────────────────

    @Override
    public void refreshBotMemory() {
        if (!refreshing.compareAndSet(false, true)) return;
        try {
            Set<String>   visited = new LinkedHashSet<>();
            StringBuilder data    = new StringBuilder();
            crawlWebsite(baseUrl, visited, data, 0);
            String raw     = data.toString();
            String trimmed = raw.length() > 60_000 ? raw.substring(0, 60_000) + "\n[truncated]" : raw;
            this.knowledgeBase = trimmed;
            this.lastRefreshed = Instant.now();
            this.chatClient    = builder
                    .defaultSystem(BASE_SYSTEM + "\n\n=== NUTRITAP KNOWLEDGE ===\n" + trimmed)
                    .build();
            System.out.printf("[NutriBot] Refreshed: %d pages, %,d chars%n", visited.size(), trimmed.length());
        } catch (Exception e) {
            System.err.println("[NutriBot] Refresh error: " + e.getMessage());
        } finally {
            refreshing.set(false);
        }
    }

    private void crawlWebsite(String url, Set<String> visited, StringBuilder data, int depth) {
        if (depth > 2 || visited.size() >= 25 || visited.contains(url)
                || url.matches(".*\\.(jpg|jpeg|png|gif|pdf|zip|mp4|svg|ico|css|js)$")
                || url.contains("mailto:") || url.contains("tel:")) return;
        try {
            visited.add(url);
            Document doc = Jsoup.connect(url).sslSocketFactory(trustAllFactory())
                    .userAgent("NutriBot/2.0").timeout(10_000).get();
            Elements content = doc.select("article, main, section, h1, h2, h3, p, li, td, th");
            data.append("\n\n[PAGE: ").append(url).append("]\n");
            for (Element e : content) {
                String t = e.text().trim();
                if (t.length() > 25) data.append(t).append(" ");
            }
            for (Element link : doc.select("a[href]")) {
                String next = link.attr("abs:href");
                if (next.startsWith(baseUrl) && !next.contains("#"))
                    crawlWebsite(next, visited, data, depth + 1);
            }
        } catch (Exception e) {
            System.err.println("[NutriBot] Crawl error " + url + ": " + e.getMessage());
        }
    }

    // ── CHAT ──────────────────────────────────────────────────────────────────

    @Override
    public String getChatResponse(String query) {
        return getChatResponse(query, "default");
    }

    @Override
    public String getChatResponse(String query, String sessionId) {
        String intent = detectIntent(query);
        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        if (history.size() > 16) {
            history = new ArrayList<>(history.subList(history.size() - 16, history.size()));
            sessions.put(sessionId, history);
        }
        String fullQuery = intent.isEmpty() ? query : intent + "\n\nUser: " + query;
        if (isAngry(history)) fullQuery += "\n[NOTE: User frustrated — proactively offer live agent.]";
        try {
            String resp = chatClient.prompt().user(fullQuery).call().content();
            Map<String, String> u = new HashMap<>(); u.put("role", "user");      u.put("content", query);
            Map<String, String> b = new HashMap<>(); b.put("role", "assistant"); b.put("content", resp);
            history.add(u); history.add(b);
            return resp;
        } catch (Exception e) {
            return "I'm having trouble connecting right now. Please try again or speak with a live agent.";
        }
    }

    public void clearSession(String sessionId) { sessions.remove(sessionId); }

    // ── STATUS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getStatus() {
        return Map.of(
                "refreshing",     refreshing.get(),
                "lastRefreshed",  lastRefreshed != null ? lastRefreshed.toString() : "never",
                "knowledgeChars", knowledgeBase.length(),
                "activeSessions", sessions.size()
        );
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private String detectIntent(String q) {
        String l = q.toLowerCase();
        if (l.matches(".*\\b(refund|money|charge|payment|deduct|billed|paid|upi|transaction)\\b.*")) return REFUND_CTX;
        if (l.matches(".*\\b(kiosk|machine|dispens|screen|offline|error|vend)\\b.*"))               return KIOSK_CTX;
        if (l.matches(".*\\b(franchise|partner|invest|open|location|business|earn)\\b.*"))          return FRANCHISE_CTX;
        return "";
    }

    private boolean isAngry(List<Map<String, String>> history) {
        return history.stream()
                .filter(m -> "user".equals(m.get("role")))
                .filter(m -> m.getOrDefault("content","").toLowerCase()
                        .matches(".*\\b(terrible|fraud|scam|useless|angry|worst|hate|disgusting)\\b.*"))
                .count() >= 2;
    }

    private static SSLSocketFactory trustAllFactory() {
        try {
            TrustManager[] tm = { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tm, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) { return (SSLSocketFactory) SSLSocketFactory.getDefault(); }
    }
}