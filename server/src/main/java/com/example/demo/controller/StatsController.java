package com.example.demo.controller;

import com.example.demo.model.dto.ApiResponse;
import com.example.demo.service.JycleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final JycleService jycleService;

    public StatsController(JycleService jycleService) {
        this.jycleService = jycleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampusStats() {
        return ResponseEntity.ok(ApiResponse.ok(jycleService.getCampusStats()));
    }
}
