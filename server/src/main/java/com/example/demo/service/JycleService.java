package com.example.demo.service;

import com.example.demo.model.Cycle;
import com.example.demo.model.CycleStatus;
import com.example.demo.model.Hub;
import com.example.demo.model.Rental;
import com.example.demo.model.RentalStatus;
import com.example.demo.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class JycleService {

    private final Map<String, Hub> hubs = new ConcurrentHashMap<>();
    private final Map<String, Cycle> cycles = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Rental> activeRentals = new ConcurrentHashMap<>();
    private final List<Rental> rentalHistory = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void initSeedData() {
        // Initialize Campus Hubs
        registerHub(new Hub("HUB-LIB", "Library Hub", "Central Library Portico", 15, 0));
        registerHub(new Hub("HUB-HOSTEL-B", "Hostel Block B Hub", "Hostel 2 Courtyard", 12, 0));
        registerHub(new Hub("HUB-ENG-QUAD", "Engineering Quad Hub", "CS & Mechanical Quadrangle", 10, 0));
        registerHub(new Hub("HUB-SPORTS", "Sports Complex Hub", "Gymnasium & Athletics Arena", 12, 0));
        registerHub(new Hub("HUB-MAIN-GATE", "Main Campus Gate Hub", "Gate 1 Security & Visitor Plaza", 15, 0));

        // Initialize Cycles distributed across hubs
        String[] models = {"Jycle Classic 7-Speed", "Jycle Cruiser", "Jycle Volt E-Assist"};
        
        seedCycle("JC-101", models[0], "HUB-LIB");
        seedCycle("JC-102", models[1], "HUB-LIB");
        seedCycle("JC-103", models[0], "HUB-LIB");
        seedCycle("JC-104", models[2], "HUB-LIB");
        seedCycle("JC-105", models[1], "HUB-LIB");
        seedCycle("JC-106", models[0], "HUB-LIB");
        seedCycle("JC-107", models[2], "HUB-LIB");
        seedCycle("JC-108", models[0], "HUB-LIB");

        seedCycle("JC-201", models[0], "HUB-HOSTEL-B");
        seedCycle("JC-202", models[1], "HUB-HOSTEL-B");
        seedCycle("JC-203", models[2], "HUB-HOSTEL-B");
        seedCycle("JC-204", models[0], "HUB-HOSTEL-B");
        seedCycle("JC-205", models[1], "HUB-HOSTEL-B");

        seedCycle("JC-301", models[0], "HUB-ENG-QUAD");
        seedCycle("JC-302", models[2], "HUB-ENG-QUAD");

        seedCycle("JC-401", models[1], "HUB-SPORTS");
        seedCycle("JC-402", models[0], "HUB-SPORTS");
        seedCycle("JC-403", models[2], "HUB-SPORTS");
        seedCycle("JC-404", models[0], "HUB-SPORTS");

        seedCycle("JC-501", models[0], "HUB-MAIN-GATE");
        seedCycle("JC-502", models[1], "HUB-MAIN-GATE");
        seedCycle("JC-503", models[2], "HUB-MAIN-GATE");
        seedCycle("JC-504", models[0], "HUB-MAIN-GATE");
        seedCycle("JC-505", models[1], "HUB-MAIN-GATE");
        seedCycle("JC-506", models[0], "HUB-MAIN-GATE");

        // Pre-seeded default student user
        User defaultUser = new User("USR-01", "Omkar", "omkar@college.edu", "CY2024-042", 14, null);
        users.put(defaultUser.id(), defaultUser);
        users.put(defaultUser.email().toLowerCase(), defaultUser);

        // Historical sample rental
        Rental sampleRide = new Rental(
            "RENT-PAST-01",
            "USR-01",
            "JC-101",
            "HUB-MAIN-GATE",
            "Main Campus Gate Hub",
            Instant.now().minusSeconds(7200),
            "HUB-LIB",
            "Library Hub",
            Instant.now().minusSeconds(5400),
            RentalStatus.COMPLETED
        );
        rentalHistory.add(sampleRide);
    }

    private void registerHub(Hub hub) {
        hubs.put(hub.id(), hub);
    }

    private void seedCycle(String cycleId, String model, String hubId) {
        Hub hub = hubs.get(hubId);
        String hubName = hub != null ? hub.name() : "Campus Hub";
        cycles.put(cycleId, new Cycle(cycleId, model, CycleStatus.AVAILABLE, hubId, hubName));
        if (hub != null) {
            hubs.put(hubId, hub.withAvailableCycles(hub.availableCycles() + 1));
        }
    }

    // --- Authentication ---
    public User authenticateCollegeUser(String email, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        
        String cleanEmail = email.trim().toLowerCase();
        // Check college institutional domain
        if (!cleanEmail.endsWith(".edu") && !cleanEmail.endsWith("college.edu") && !cleanEmail.contains("@college.")) {
            // For campus flexibility, accept if it has valid email format or ends with college domain
            if (!cleanEmail.contains("@")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
        }

        // Check if existing user
        User existing = users.get(cleanEmail);
        if (existing != null) {
            Rental active = activeRentals.get(existing.id());
            return existing.withActiveRental(active);
        }

        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String rollNumber = "CY" + (2024 + (users.size() % 3)) + "-" + String.format("%03d", users.size() + 1);
        String displayName = (name != null && !name.isBlank()) ? name : cleanEmail.split("@")[0];

        User newUser = new User(userId, displayName, cleanEmail, rollNumber, 0, null);
        users.put(userId, newUser);
        users.put(cleanEmail, newUser);
        return newUser;
    }

    public Optional<User> getUserByIdOrEmail(String identifier) {
        if (identifier == null) return Optional.empty();
        User u = users.get(identifier.trim().toLowerCase());
        if (u == null) {
            u = users.get(identifier.trim());
        }
        if (u != null) {
            Rental active = activeRentals.get(u.id());
            return Optional.of(u.withActiveRental(active));
        }
        return Optional.empty();
    }

    // --- Hubs ---
    public List<Hub> getAllHubs() {
        // Refresh available counts dynamically based on cycle statuses
        return hubs.values().stream()
            .map(hub -> {
                int count = (int) cycles.values().stream()
                    .filter(c -> hub.id().equals(c.hubId()) && c.status() == CycleStatus.AVAILABLE)
                    .count();
                return hub.withAvailableCycles(count);
            })
            .sorted(Comparator.comparing(Hub::name))
            .collect(Collectors.toList());
    }

    public Optional<Hub> getHubById(String hubId) {
        Hub hub = hubs.get(hubId);
        if (hub == null) return Optional.empty();
        int count = (int) cycles.values().stream()
            .filter(c -> hub.id().equals(c.hubId()) && c.status() == CycleStatus.AVAILABLE)
            .count();
        return Optional.of(hub.withAvailableCycles(count));
    }

    // --- Cycles ---
    public List<Cycle> getCycles(String hubId, CycleStatus status) {
        return cycles.values().stream()
            .filter(c -> hubId == null || hubId.equalsIgnoreCase(c.hubId()))
            .filter(c -> status == null || c.status() == status)
            .sorted(Comparator.comparing(Cycle::id))
            .collect(Collectors.toList());
    }

    public Optional<Cycle> getCycleById(String cycleId) {
        return Optional.ofNullable(cycles.get(cycleId));
    }

    // --- Rental Lifecycle ---
    public synchronized Rental unlockCycle(String cycleId, String userId) {
        Cycle cycle = cycles.get(cycleId);
        if (cycle == null) {
            throw new NoSuchElementException("Cycle not found with ID: " + cycleId);
        }

        if (cycle.status() != CycleStatus.AVAILABLE) {
            throw new IllegalStateException("Cycle " + cycleId + " is currently " + cycle.status());
        }

        Optional<User> userOpt = getUserByIdOrEmail(userId);
        if (userOpt.isEmpty()) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        User user = userOpt.get();

        if (activeRentals.containsKey(user.id())) {
            throw new IllegalStateException("User " + user.name() + " already has an active cycle rental.");
        }

        String startHubId = cycle.hubId();
        String startHubName = cycle.hubName();

        // Update cycle state
        Cycle inUseCycle = cycle.withStatusAndHub(CycleStatus.IN_USE, null, "In Transit");
        cycles.put(cycleId, inUseCycle);

        // Update hub count
        if (startHubId != null && hubs.containsKey(startHubId)) {
            Hub h = hubs.get(startHubId);
            hubs.put(startHubId, h.withAvailableCycles(Math.max(0, h.availableCycles() - 1)));
        }

        // Create rental record
        String rentalId = "RENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Rental rental = new Rental(
            rentalId,
            user.id(),
            cycleId,
            startHubId,
            startHubName,
            Instant.now(),
            null,
            null,
            null,
            RentalStatus.ACTIVE
        );

        activeRentals.put(user.id(), rental);
        users.put(user.id(), user.withActiveRental(rental));
        users.put(user.email().toLowerCase(), user.withActiveRental(rental));

        return rental;
    }

    public synchronized Rental returnCycle(String cycleId, String hubId, String userId) {
        Cycle cycle = cycles.get(cycleId);
        if (cycle == null) {
            throw new NoSuchElementException("Cycle not found with ID: " + cycleId);
        }

        if (cycle.status() != CycleStatus.IN_USE) {
            throw new IllegalStateException("Cycle " + cycleId + " is not currently in use.");
        }

        Hub targetHub = hubs.get(hubId);
        if (targetHub == null) {
            throw new NoSuchElementException("Target hub not found: " + hubId);
        }

        Optional<User> userOpt = getUserByIdOrEmail(userId);
        if (userOpt.isEmpty()) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        User user = userOpt.get();

        Rental active = activeRentals.get(user.id());
        if (active == null || !active.cycleId().equalsIgnoreCase(cycleId)) {
            throw new IllegalStateException("No active rental found for cycle " + cycleId + " under user " + user.name());
        }

        // Update cycle
        Cycle returnedCycle = cycle.withStatusAndHub(CycleStatus.AVAILABLE, targetHub.id(), targetHub.name());
        cycles.put(cycleId, returnedCycle);

        // Update hub count
        hubs.put(targetHub.id(), targetHub.withAvailableCycles(targetHub.availableCycles() + 1));

        // Complete rental
        Rental completed = active.complete(targetHub.id(), targetHub.name(), Instant.now());
        activeRentals.remove(user.id());
        rentalHistory.add(completed);

        // Update user stats
        User updatedUser = user.withCompletedRental(user.ridesCount() + 1);
        users.put(user.id(), updatedUser);
        users.put(user.email().toLowerCase(), updatedUser);

        return completed;
    }

    public Optional<Rental> getActiveRentalForUser(String userId) {
        Optional<User> user = getUserByIdOrEmail(userId);
        return user.flatMap(u -> Optional.ofNullable(activeRentals.get(u.id())));
    }

    public List<Rental> getUserRentalHistory(String userId) {
        Optional<User> user = getUserByIdOrEmail(userId);
        if (user.isEmpty()) return Collections.emptyList();
        String uid = user.get().id();
        return rentalHistory.stream()
            .filter(r -> uid.equals(r.userId()))
            .sorted(Comparator.comparing(Rental::startTime).reversed())
            .collect(Collectors.toList());
    }

    // --- Campus Overview Stats ---
    public Map<String, Object> getCampusStats() {
        long total = cycles.size();
        long inUse = cycles.values().stream().filter(c -> c.status() == CycleStatus.IN_USE).count();
        long available = cycles.values().stream().filter(c -> c.status() == CycleStatus.AVAILABLE).count();
        long maintenance = cycles.values().stream().filter(c -> c.status() == CycleStatus.MAINTENANCE).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCycles", total);
        stats.put("availableCycles", available);
        stats.put("inUseCycles", inUse);
        stats.put("maintenanceCycles", maintenance);
        stats.put("totalHubs", hubs.size());
        stats.put("totalCompletedRides", rentalHistory.size());
        stats.put("registeredUsers", (long) (users.size() / 2)); // keyed by id and email
        return stats;
    }
}
