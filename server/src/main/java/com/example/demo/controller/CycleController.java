package com.example.demo.controller;

import com.example.demo.model.Cycle;
import com.example.demo.model.CycleStatus;
import com.example.demo.model.dto.ApiResponse;
import com.example.demo.service.JycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cycles")
public class CycleController {

    private final JycleService jycleService;

    public CycleController(JycleService jycleService) {
        this.jycleService = jycleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Cycle>>> getCycles(
        @RequestParam(required = false) String hubId,
        @RequestParam(required = false) CycleStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(jycleService.getCycles(hubId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Cycle>> getCycleById(@PathVariable String id) {
        return jycleService.getCycleById(id)
            .map(c -> ResponseEntity.ok(ApiResponse.ok(c)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Cycle not found with ID: " + id)));
    }
}
