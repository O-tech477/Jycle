package com.example.demo.model;

public record User(
    String id,
    String name,
    String email,
    String rollNo,
    int ridesCount,
    Rental activeRental
) {
    public User withActiveRental(Rental rental) {
        return new User(this.id, this.name, this.email, this.rollNo, this.ridesCount, rental);
    }

    public User withCompletedRental(int newRidesCount) {
        return new User(this.id, this.name, this.email, this.rollNo, newRidesCount, null);
    }
}
