package com.example.demo.model;

public record Hub(
    String id,
    String name,
    String locationDescription,
    int capacity,
    int availableCycles
) {
    public Hub withAvailableCycles(int count) {
        return new Hub(this.id, this.name, this.locationDescription, this.capacity, count);
    }
}
