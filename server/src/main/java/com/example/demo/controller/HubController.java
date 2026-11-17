package com.example.demo.controller;

import com.example.demo.model.Cycle;
import com.example.demo.model.CycleStatus;
import com.example.demo.model.Hub;
import com.example.demo.model.dto.ApiResponse;
import com.example.demo.service.JycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
public class HubController {

    private final JycleService jycleService;

    public HubController(JycleService jycleService) {
        this.jycleService = jycleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Hub>>> getAllHubs() {
        return ResponseEntity.ok(ApiResponse.ok(jycleService.getAllHubs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Hub>> getHubById(@PathVariable String id) {
        return jycleService.getHubById(id)
            .map(h -> ResponseEntity.ok(ApiResponse.ok(h)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Hub not found with ID: " + id)));
    }

    @GetMapping("/{id}/cycles")
    public ResponseEntity<ApiResponse<List<Cycle>>> getCyclesAtHub(@PathVariable String id) {
        return jycleService.getHubById(id)
            .map(h -> ResponseEntity.ok(ApiResponse.ok(jycleService.getCycles(id, CycleStatus.AVAILABLE))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Hub not found with ID: " + id)));
    }
}
