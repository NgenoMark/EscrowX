package com.example.escbackend.escrow.controller;

import com.example.escbackend.escrow.dto.TransactionStatusHistoryResponse;
import com.example.escbackend.escrow.service.TransactionStatusHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionStatusHistoryController {

    private final TransactionStatusHistoryService transactionStatusHistoryService;

    public TransactionStatusHistoryController(TransactionStatusHistoryService transactionStatusHistoryService) {
        this.transactionStatusHistoryService = transactionStatusHistoryService;
    }

    @GetMapping("/{transactionId}/status-history")
    public List<TransactionStatusHistoryResponse> getByTransactionId(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return transactionStatusHistoryService.getHistoryByTransactionId(transactionId, actorUserId);
    }

    @GetMapping("/status-history")
    public List<TransactionStatusHistoryResponse> getAllHistory(
            @RequestParam(defaultValue = "desc") String order,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId
    ) {
        return transactionStatusHistoryService.getAllHistory(order, actorUserId);
    }
}
