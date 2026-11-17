package com.example.demo;

import com.example.demo.controller.*;
import com.example.demo.model.Cycle;
import com.example.demo.model.CycleStatus;
import com.example.demo.model.Rental;
import com.example.demo.model.User;
import com.example.demo.model.dto.*;
import com.example.demo.service.JycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private JycleService jycleService;

    @Autowired
    private AuthController authController;

    @Autowired
    private HubController hubController;

    @Autowired
    private CycleController cycleController;

    @Autowired
    private RentalController rentalController;

    @Autowired
    private StatsController statsController;

    @Test
    void contextLoads() {
        assertNotNull(jycleService);
        assertNotNull(authController);
        assertNotNull(hubController);
        assertNotNull(cycleController);
        assertNotNull(rentalController);
        assertNotNull(statsController);
    }

    @Test
    void testHubsAndCyclesSeedData() {
        var hubs = jycleService.getAllHubs();
        assertFalse(hubs.isEmpty(), "Hubs should be populated");
        assertTrue(hubs.size() >= 5, "At least 5 hubs should exist");

        List<Cycle> cycles = jycleService.getCycles(null, null);
        assertFalse(cycles.isEmpty(), "Cycles should be populated");
        assertTrue(cycles.stream().anyMatch(c -> c.id().equals("JC-101")));
    }

    @Test
    void testCollegeUserAuthentication() {
        User user = jycleService.authenticateCollegeUser("student@college.edu", "Campus Student");
        assertNotNull(user);
        assertEquals("student@college.edu", user.email());
        assertTrue(user.rollNo().startsWith("CY"));
    }

    @Test
    void testRentalCycleFlow() {
        // Authenticate a fresh test user
        User user = jycleService.authenticateCollegeUser("rider.test@college.edu", "Rider Test");

        // Pick an available cycle
        List<Cycle> availableCycles = jycleService.getCycles("HUB-SPORTS", CycleStatus.AVAILABLE);
        assertFalse(availableCycles.isEmpty(), "Should have available cycles at sports hub");
        Cycle targetCycle = availableCycles.get(0);

        // Unlock
        Rental rental = jycleService.unlockCycle(targetCycle.id(), user.id());
        assertNotNull(rental);
        assertEquals(targetCycle.id(), rental.cycleId());
        assertEquals("HUB-SPORTS", rental.startHubId());

        // Verify active rental is registered
        var activeOpt = jycleService.getActiveRentalForUser(user.id());
        assertTrue(activeOpt.isPresent());
        assertEquals(rental.id(), activeOpt.get().id());

        // Return to another hub (e.g. Library Hub)
        Rental completed = jycleService.returnCycle(targetCycle.id(), "HUB-LIB", user.id());
        assertNotNull(completed);
        assertEquals("HUB-LIB", completed.endHubId());
        assertNotNull(completed.endTime());

        // Verify no longer active
        assertTrue(jycleService.getActiveRentalForUser(user.id()).isEmpty());

        // Verify history recorded
        List<Rental> history = jycleService.getUserRentalHistory(user.id());
        assertTrue(history.stream().anyMatch(r -> r.id().equals(completed.id())));
    }

    @Test
    void testCampusStats() {
        Map<String, Object> stats = jycleService.getCampusStats();
        assertNotNull(stats);
        assertTrue((Long) stats.get("totalCycles") > 0);
        assertTrue((Integer) stats.get("totalHubs") >= 5);
    }

    @Test
    void testControllersDirectly() {
        // 1. HubController
        var hubsResp = hubController.getAllHubs();
        assertEquals(HttpStatus.OK, hubsResp.getStatusCode());
        assertNotNull(hubsResp.getBody());
        assertTrue(hubsResp.getBody().success());
        assertFalse(hubsResp.getBody().data().isEmpty());

        // 2. CycleController
        var cyclesResp = cycleController.getCycles(null, null);
        assertEquals(HttpStatus.OK, cyclesResp.getStatusCode());
        assertNotNull(cyclesResp.getBody());
        assertTrue(cyclesResp.getBody().success());
        assertFalse(cyclesResp.getBody().data().isEmpty());

        // 3. StatsController
        var statsResp = statsController.getCampusStats();
        assertEquals(HttpStatus.OK, statsResp.getStatusCode());
        assertNotNull(statsResp.getBody());
        assertTrue(statsResp.getBody().success());

        // 4. AuthController
        LoginRequest loginReq = new LoginRequest("test.auth@college.edu", "Auth Tester", "token123");
        var loginResp = authController.loginWithGoogle(loginReq);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertNotNull(loginResp.getBody());
        assertTrue(loginResp.getBody().success());
        assertEquals("test.auth@college.edu", loginResp.getBody().data().email());

        // 5. RentalController Unlock & Return
        User rider = loginResp.getBody().data();
        Cycle freeCycle = cyclesResp.getBody().data().stream()
            .filter(c -> c.status() == CycleStatus.AVAILABLE)
            .findFirst()
            .orElseThrow();

        // Unlock via REST Controller
        var unlockResp = rentalController.unlockCycle(new UnlockRequest(freeCycle.id(), rider.id()));
        assertEquals(HttpStatus.OK, unlockResp.getStatusCode());
        assertTrue(unlockResp.getBody().success());

        // Return via REST Controller
        var returnResp = rentalController.returnCycle(new ReturnRequest(freeCycle.id(), "HUB-LIB", rider.id()));
        assertEquals(HttpStatus.OK, returnResp.getStatusCode());
        assertTrue(returnResp.getBody().success());

        // Active rental should now be empty
        var activeResp = rentalController.getActiveRental(rider.id());
        assertEquals(HttpStatus.OK, activeResp.getStatusCode());
        assertNull(activeResp.getBody().data());
    }
}
