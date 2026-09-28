package com.boundless.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

@Slf4j
@Service
public class GeminiService {

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String ask(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Spring AI Gemini API failed: {}", e.getMessage());
            return "AI temporarily unavailable: " + e.getMessage();
        }
    }

    public String analyzeImage(byte[] imageBytes, String mimeType, String prompt) {
        try {
            return chatClient.prompt()
                    .user(u -> u.text(prompt)
                                .media(new Media(MimeTypeUtils.parseMimeType(mimeType), new ByteArrayResource(imageBytes))))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Spring AI Gemini Vision failed: {}", e.getMessage());
            return "OCR temporarily unavailable: " + e.getMessage();
        }
    }
}
