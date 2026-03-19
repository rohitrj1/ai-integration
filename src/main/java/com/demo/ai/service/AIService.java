package com.demo.ai.service;

public interface AIService {
    String getChatResponse(String query);
    String getChatResponse(String query, String sessionId);
    void refreshBotMemory();
}