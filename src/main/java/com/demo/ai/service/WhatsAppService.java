package com.demo.ai.service;

public interface WhatsAppService {
    void sendMessage(String mobile, String message);
    void sendMessageWithMedia(String mobile, String message, String mediaUrl);
}