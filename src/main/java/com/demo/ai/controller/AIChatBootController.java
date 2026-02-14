package com.demo.ai.controller;

import com.demo.ai.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


@RestController
@RequestMapping("/api/ai")
public class AIChatBootController {

    @Autowired
    private AIService aiService;


    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String q) {
        return ResponseEntity.ok(aiService.getChatResponse(q));
    }

    @PostMapping("/admin/refresh")
    public ResponseEntity<String> refresh() {
        aiService.refreshBotMemory();
        return ResponseEntity.ok("Bot memory refreshed successfully!");
    }

    @GetMapping("/index")
    public ModelAndView index() {
        return new ModelAndView("index"); // Ye 'templates/index.html' ko dhoondega
    }




//    private final ChatClient chatClient;
//
////    public AIChatBootController(ChatClient.Builder builder) {
////
////        String websiteData;
////        try {
////            // JSoup se website ka text nikalna (Vastly Secure & Fast)
////            websiteData = Jsoup.connect("https://nutritap.in/").get().text();
////        } catch (Exception e) {
////            websiteData = "NutriTap is India's leading smart retail and vending provider.";
////        }
////
////        this.chatClient = builder
////                .defaultSystem("You are the NutriTap Support Bot. Use ONLY this data: " + websiteData +
////                        ". If the answer is not there, say you only know about NutriTap.")
////                .build();
////    }
//
//
//    public AIChatBootController(ChatClient.Builder builder) {
//        String baseUrl = "https://nutritap.in";
//        Set<String> visitedLinks = new HashSet<>();
//        StringBuilder megaData = new StringBuilder();
//
//        // Pehle main pages ki list nikalte hain
//        crawlWebsite(baseUrl, baseUrl, visitedLinks, megaData, 0);
//
//        this.chatClient = builder
//                .defaultSystem("You are the NutriTap Support Bot. Here is the COMPLETE website data: " + megaData.toString() +
//                        ". Answer user queries based on this data. If you don't find it, say you only specialize in NutriTap.")
//                .build();
//    }
//
//    private void crawlWebsite(String url, String baseUrl, Set<String> visited, StringBuilder data, int depth) {
//        // Depth limit 2-3 rakhein taaki infinite loop na ho aur memory na bhare
//        if (depth > 2 || visited.contains(url) || visited.size() > 20) return;
//
//        try {
//            visited.add(url);
//            Document doc = Jsoup.connect(url).get();
//
//            // Sirf kaam ka data uthayein (Headers and Paragraphs)
//            Elements content = doc.select("h1, h2, h3, p, li");
//            data.append("\n--- Source: ").append(url).append(" ---\n");
//            for (Element element : content) {
//                data.append(element.text()).append(" ");
//            }
//
//            // Saare internal links nikal kar unpar crawl karein
//            Elements links = doc.select("a[href]");
//            for (Element link : links) {
//                String nextUrl = link.attr("abs:href");
//                if (nextUrl.startsWith(baseUrl) && !nextUrl.contains("#")) {
//                    crawlWebsite(nextUrl, baseUrl, visited, data, depth + 1);
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("Could not crawl: " + url);
//        }
//    }
//

//
//    @PostMapping("/chat")
//    @ResponseBody
//    public ResponseEntity<String> sendMessage(@RequestParam(value = "q") String q) {
//        try {
//            String content = chatClient.prompt().user(q).call().content();
//            return ResponseEntity.ok(content);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error: " + e.getMessage());
//        }
//    }
}
