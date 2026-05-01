package com.remitly.stock_exchange.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remitly.stock_exchange.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LogController {

    private final AuditService auditService;

    @GetMapping("/log")
    public Map<String, Object> getLog() {
        return Map.of("log", auditService.getAll());
    }
}