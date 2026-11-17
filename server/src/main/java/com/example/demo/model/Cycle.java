package com.example.demo.model;

public record Cycle(
    String id,
    String model,
    CycleStatus status,
    String hubId,
    String hubName
) {
    public Cycle withStatusAndHub(CycleStatus newStatus, String newHubId, String newHubName) {
        return new Cycle(this.id, this.model, newStatus, newHubId, newHubName);
    }
}
