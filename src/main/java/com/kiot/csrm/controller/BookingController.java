package com.kiot.csrm.controller;

import com.kiot.csrm.dto.ApiResponse;
import com.kiot.csrm.dto.BookingRequest;
import com.kiot.csrm.entity.Booking;
import com.kiot.csrm.entity.Role;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.service.BookingService;
import com.kiot.csrm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody BookingRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Booking booking = bookingService.createBooking(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Booking confirmed successfully", booking));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getBookings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.ok("All bookings retrieved", bookingService.getAllBookings()));
        } else {
            return ResponseEntity.ok(ApiResponse.ok("User bookings retrieved", bookingService.getUserBookings(user.getId())));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("My bookings retrieved", bookingService.getUserBookings(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking details", bookingService.getBookingById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> modifyBooking(@PathVariable Long id,
                                                              @Valid @RequestBody BookingRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Booking modified = bookingService.modifyBooking(id, user, request);
        return ResponseEntity.ok(ApiResponse.ok("Booking modified successfully", modified));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        Booking cancelled = bookingService.cancelBooking(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", cancelled));
    }

    @GetMapping("/conflict-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkConflict(
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long excludeBookingId) {

        boolean hasConflict = bookingService.checkConflict(resourceId, startTime, endTime, excludeBookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("resourceId", resourceId);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("hasConflict", hasConflict);
        result.put("isAvailable", !hasConflict);

        String message = hasConflict ? "Slot is already booked" : "Slot is available";
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }
}
