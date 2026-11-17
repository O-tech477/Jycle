package com.example.demo.model;

import java.time.Instant;

public record Rental(
    String id,
    String userId,
    String cycleId,
    String startHubId,
    String startHubName,
    Instant startTime,
    String endHubId,
    String endHubName,
    Instant endTime,
    RentalStatus status
) {
    public Rental complete(String endHubId, String endHubName, Instant endTime) {
        return new Rental(
            this.id,
            this.userId,
            this.cycleId,
            this.startHubId,
            this.startHubName,
            this.startTime,
            endHubId,
            endHubName,
            endTime,
            RentalStatus.COMPLETED
        );
    }
}
