package com.remitly.stock_exchange.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChaosController {

    @PostMapping("/chaos")
    public void chaos() {
        System.exit(0);
    }
}