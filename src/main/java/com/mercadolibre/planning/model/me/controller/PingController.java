package com.mercadolibre.planning.model.me.controller;

import com.newrelic.api.agent.NewRelic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class PingController {

    @GetMapping
    public ResponseEntity<?> handlePing() {
        NewRelic.ignoreTransaction();
        return ResponseEntity.ok("pong");
    }
}
