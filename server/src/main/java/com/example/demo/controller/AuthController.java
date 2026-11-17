package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.dto.ApiResponse;
import com.example.demo.model.dto.LoginRequest;
import com.example.demo.service.JycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JycleService jycleService;

    public AuthController(JycleService jycleService) {
        this.jycleService = jycleService;
    }

    @PostMapping("/auth/google")
    public ResponseEntity<ApiResponse<User>> loginWithGoogle(@RequestBody LoginRequest request) {
        try {
            User user = jycleService.authenticateCollegeUser(request.email(), request.name());
            return ResponseEntity.ok(ApiResponse.ok("Login successful", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{identifier}")
    public ResponseEntity<ApiResponse<User>> getUserProfile(@PathVariable String identifier) {
        return jycleService.getUserByIdOrEmail(identifier)
            .map(u -> ResponseEntity.ok(ApiResponse.ok(u)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("User not found: " + identifier)));
    }
}
