package com.example.demo.controller;

import com.example.demo.model.Rental;
import com.example.demo.model.dto.ApiResponse;
import com.example.demo.model.dto.ReturnRequest;
import com.example.demo.model.dto.UnlockRequest;
import com.example.demo.service.JycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final JycleService jycleService;

    public RentalController(JycleService jycleService) {
        this.jycleService = jycleService;
    }

    @PostMapping("/unlock")
    public ResponseEntity<ApiResponse<Rental>> unlockCycle(@RequestBody UnlockRequest request) {
        try {
            Rental rental = jycleService.unlockCycle(request.cycleId(), request.userId());
            return ResponseEntity.ok(ApiResponse.ok("Cycle unlocked successfully. Have a safe ride!", rental));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unlock failed: " + e.getMessage()));
        }
    }

    @PostMapping("/return")
    public ResponseEntity<ApiResponse<Rental>> returnCycle(@RequestBody ReturnRequest request) {
        try {
            Rental rental = jycleService.returnCycle(request.cycleId(), request.hubId(), request.userId());
            return ResponseEntity.ok(ApiResponse.ok("Cycle returned successfully. Thank you for using Jycle!", rental));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Return failed: " + e.getMessage()));
        }
    }

    @GetMapping("/active/{userId}")
    public ResponseEntity<ApiResponse<Rental>> getActiveRental(@PathVariable String userId) {
        return jycleService.getActiveRentalForUser(userId)
            .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
            .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok("No active rental", null)));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<Rental>>> getRentalHistory(@PathVariable String userId) {
        List<Rental> history = jycleService.getUserRentalHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
