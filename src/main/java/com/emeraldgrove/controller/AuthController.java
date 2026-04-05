package com.emeraldgrove.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/check")
    public ResponseEntity<Map<String, String>> check() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
