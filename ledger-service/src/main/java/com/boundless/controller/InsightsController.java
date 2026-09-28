package com.boundless.controller;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import com.boundless.service.GeminiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final TransactionRepository transactionRepository;
    private final GeminiService geminiService;

    // Feature 28+29: AI-Powered Natural Language Query + Storytelling
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody QuestionRequest req) {
        List<Transaction> txns = transactionRepository.findAll();
        String data = buildDataContext(txns);

        String prompt = "You are a senior financial analyst for Boundless FinTech corporate card platform.\n" +
                "Answer the user's question based ONLY on this transaction data.\n" +
                "Be specific with numbers, percentages, and trends. Use emojis for visual appeal.\n\n" +
                data + "\n\nUser Question: " + req.getQuestion();

        return ResponseEntity.ok(Map.of(
                "question", req.getQuestion(),
                "answer", geminiService.ask(prompt),
                "transactionsAnalyzed", txns.size()
        ));
    }

    // Feature 28: "Where Did The Money Go?" narrative report
    @GetMapping("/story")
    public ResponseEntity<?> story() {
        List<Transaction> txns = transactionRepository.findAll();
        String data = buildDataContext(txns);

        String prompt = "You are a CFO's financial analyst at Boundless FinTech.\n" +
                "Write a compelling narrative spending report titled 'Where Did The Money Go?'\n" +
                "Include: Key highlights with exact amounts, category breakdown by merchant, red flags (unusual patterns), " +
                "top spenders by card, and actionable cost-saving suggestions.\n" +
                "Use emojis. Be specific with dollar amounts and percentages.\n\n" +
                "Transaction Data:\n" + data;

        return ResponseEntity.ok(Map.of("report", geminiService.ask(prompt), "transactionsAnalyzed", txns.size()));
    }

    private String buildDataContext(List<Transaction> txns) {
        StringBuilder sb = new StringBuilder();
        for (Transaction tx : txns) {
            sb.append(String.format("- %s | Card:%s | $%s %s | Merchant:%s | Status:%s | Receipt:%s\n",
                    tx.getTimestamp(), tx.getCardNumber(), tx.getAmount(),
                    tx.getOriginalCurrency() != null ? tx.getOriginalCurrency() : "USD",
                    tx.getMerchantName(), tx.getStatus(), tx.isReceiptUploaded()));
        }
        return sb.toString();
    }
}

@Data
class QuestionRequest {
    private String question;
}
