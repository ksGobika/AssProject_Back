package com.kiot.csrm.controller;

import com.kiot.csrm.dto.ApiResponse;
import com.kiot.csrm.dto.ResourceRequest;
import com.kiot.csrm.entity.Resource;
import com.kiot.csrm.entity.Role;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.service.BookingService;
import com.kiot.csrm.service.ResourceService;
import com.kiot.csrm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ResourceService resourceService;
    private final BookingService bookingService;

    public AdminController(UserService userService, ResourceService resourceService, BookingService bookingService) {
        this.userService = userService;
        this.resourceService = resourceService;
        this.bookingService = bookingService;
    }

    // --- User Management ---
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", userService.getAllUsers()));
    }

    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<User>>> getPendingUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Pending users retrieved", userService.getPendingUsers()));
    }

    @PutMapping("/users/{id}/approve")
    public ResponseEntity<ApiResponse<User>> approveUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        User user = userService.approveUser(id, admin);
        return ResponseEntity.ok(ApiResponse.ok("User '" + user.getUsername() + "' approved successfully", user));
    }

    @PutMapping("/users/{id}/reject")
    public ResponseEntity<ApiResponse<User>> rejectUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        User user = userService.rejectUser(id, admin);
        return ResponseEntity.ok(ApiResponse.ok("User '" + user.getUsername() + "' rejected", user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<User>> updateUserRole(@PathVariable Long id,
                                                            @RequestParam Role role,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        User updated = userService.changeUserRole(id, role, admin);
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", updated));
    }

    // --- Resource Management ---
    @PostMapping("/resources")
    public ResponseEntity<ApiResponse<Resource>> createResource(@Valid @RequestBody ResourceRequest request,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        Resource resource = resourceService.createResource(request, admin);
        return ResponseEntity.ok(ApiResponse.ok("Resource created successfully", resource));
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<Resource>> updateResource(@PathVariable Long id,
                                                                @Valid @RequestBody ResourceRequest request,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        Resource updated = resourceService.updateResource(id, request, admin);
        return ResponseEntity.ok(ApiResponse.ok("Resource updated successfully", updated));
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userService.getUserByUsername(userDetails.getUsername());
        resourceService.deleteResource(id, admin);
        return ResponseEntity.ok(ApiResponse.ok("Resource deleted successfully"));
    }

    // --- Reports & Analytics ---
    @GetMapping("/reports/utilization")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUtilizationReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Map<String, Object>> report = bookingService.getUtilizationReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Utilization report generated", report));
    }

    @GetMapping("/reports/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        Map<String, Object> analytics = bookingService.getAnalyticsSummary();
        return ResponseEntity.ok(ApiResponse.ok("Analytics data retrieved", analytics));
    }
}
