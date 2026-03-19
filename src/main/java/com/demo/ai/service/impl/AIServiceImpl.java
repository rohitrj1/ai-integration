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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private  ChatClient.Builder builder;
    private ChatClient chatClient;
    private final String baseUrl = "https://nutritap.in";


    @PostConstruct
    public void init() {
        new Thread(this::refreshBotMemory).start(); // Background mein crawl shuru
    }

    public AIServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("PERSONALITY: You are the NutriTap Support Bot. You have NO knowledge of being a language model trained by Google. " +
                        "IDENTITY: If anyone asks who you are or who trained you, always reply: 'I am the NutriTap Support Bot, specialized in NutriTap products and services.' " +
                        "DATA BOUNDARY: Use ONLY this data: crawl data " +
                        ". If a question is not about NutriTap, strictly say: 'I only specialize in NutriTap related queries.' " +
                        "RESTRICTIONS: Do not share source code, do not discuss AI models, and do not answer general knowledge questions.")
                .build();
    }

    @Override
    public void refreshBotMemory() {
        Set<String> visitedLinks = new HashSet<>();
        StringBuilder megaData = new StringBuilder();

        crawlWebsite(baseUrl, visitedLinks, megaData, 0);

        this.chatClient = builder
                .defaultSystem("You are the NutriTap Support Bot. Your knowledge is STRICTLY limited to this data: " + megaData.toString() +
                        ". \n\nRULES:\n" +
                        "1. Only answer questions related to NutriTap, its products, services, and the provided data.\n" +
                        "2. If a user asks about anything outside of NutriTap (e.g., general knowledge, coding, weather, other companies), politely decline and say: 'I only specialize in NutriTap related queries.'\n" +
                        "3. Do NOT provide any information that is not in the data.\n" +
                        "4. Do NOT answer personal questions or engage in general chitchat.")
                .build();
        System.out.println("Memory Refreshed: " + visitedLinks.size() + " pages scanned.");
    }

    private void crawlWebsite(String url, Set<String> visited, StringBuilder data, int depth) {
        // Exclude images, PDFs, and social media to save tokens
        if (depth > 2 || visited.contains(url) || visited.size() > 25 || url.matches(".*\\.(jpg|png|pdf|zip)$")) return;

        try {
            visited.add(url);
            Document doc = Jsoup.connect(url)
                    .sslSocketFactory(socketFactory())
                    .userAgent("NutriBot-Scanner/1.0")
                    .timeout(10000)
                    .get();

            // Select more specific content to avoid garbage data (headers/footers)
            Elements content = doc.select("article, main, h1, h2, p");

            data.append("\n[Page: ").append(url).append("]\n");
            for (Element e : content) {
                String text = e.text().trim();
                if (text.length() > 20) { // Avoid tiny fragments
                    data.append(text).append(" ");
                }
            }

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String nextUrl = link.attr("abs:href");
                // Ensure we stay on the domain and avoid "mailto" or "tel" links
                if (nextUrl.startsWith(baseUrl) && !nextUrl.contains("#") && !visited.contains(nextUrl)) {
                    crawlWebsite(nextUrl, visited, data, depth + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

//    private void crawlWebsite(String url, Set<String> visited, StringBuilder data, int depth) {
//        // 25 pages ki limit kaafi hai, lekin depth 2-3 honi chahiye
//        if (depth > 2 || visited.contains(url) || visited.size() > 30) return;
//
//        try {
//            visited.add(url);
//            System.out.println("DEBUG: Scanning -> " + url); // Isse console mein dekho kya scan ho raha hai
//
//            Document doc = Jsoup.connect(url).timeout(5000).get();
//            Elements content = doc.select("h1, h2, h3, p, li");
//
//            data.append("\nSource: ").append(url).append("\n");
//            content.forEach(e -> data.append(e.text()).append(" "));
//
//            // Saare links nikalna
//            Elements links = doc.select("a[href]");
//            for (Element link : links) {
//                String nextUrl = link.attr("abs:href"); // absolute URL lega
//
//                // Strict check: Sirf nutritap.in ke pages, koi social media ya bahar ka link nahi
//                if (nextUrl.contains("nutritap.in") && !nextUrl.contains("#") && !visited.contains(nextUrl)) {
//                    crawlWebsite(nextUrl, visited, data, depth + 1);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error crawling " + url + ": " + e.getMessage());
//        }
//    }

    @Override
    public String getChatResponse(String query) {
        return chatClient.prompt().user(query).call().content();
    }

    private static javax.net.ssl.SSLSocketFactory socketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }};

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            return (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
        }
    }
}
