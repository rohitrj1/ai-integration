package com.demo.ai.service;

public interface AIService {

    void refreshBotMemory();

    String getChatResponse(String query);
}
