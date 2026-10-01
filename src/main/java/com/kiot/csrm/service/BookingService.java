package com.kiot.csrm.service;

import com.kiot.csrm.dto.BookingRequest;
import com.kiot.csrm.entity.*;
import com.kiot.csrm.exception.BadRequestException;
import com.kiot.csrm.exception.BookingConflictException;
import com.kiot.csrm.exception.ResourceNotFoundException;
import com.kiot.csrm.repository.BookingRepository;
import com.kiot.csrm.repository.ResourceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public BookingService(BookingRepository bookingRepository,
                          ResourceRepository resourceRepository,
                          AuditLogService auditLogService,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Booking createBooking(User user, BookingRequest request) {
        validateBookingTimes(request.getStartTime(), request.getEndTime());

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        if (!Boolean.TRUE.equals(resource.getAvailability())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently unavailable for booking");
        }

        // Detect overlapping bookings
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                null
        );

        if (!conflicts.isEmpty()) {
            Booking conflict = conflicts.get(0);
            throw new BookingConflictException("Resource '" + resource.getName() +
                    "' is already booked between " +
                    conflict.getStartTime().format(TIME_FORMATTER) + " and " +
                    conflict.getEndTime().format(TIME_FORMATTER));
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPurpose(request.getPurpose());
        booking.setRequestedServices(request.getRequestedServices());

        Booking saved = bookingRepository.save(booking);

        // Audit Log
        auditLogService.logAction(user, "BOOKING_CREATED",
                "Booking #" + saved.getId() + " confirmed for " + resource.getName() +
                " from " + saved.getStartTime().format(TIME_FORMATTER) + " to " + saved.getEndTime().format(TIME_FORMATTER));

        // Notification: Booking Confirmation
        notificationService.sendNotification(
                user,
                "Booking Confirmed: " + resource.getName(),
                "Your booking for " + resource.getName() + " (" + resource.getLocation() + ") is confirmed for " +
                        saved.getStartTime().format(TIME_FORMATTER) + " - " + saved.getEndTime().format(TIME_FORMATTER) + ".",
                "CONFIRMATION",
                "EMAIL_SMS"
        );

        return saved;
    }

    @Transactional
    public Booking modifyBooking(Long bookingId, User user, BookingRequest request) {
        validateBookingTimes(request.getStartTime(), request.getEndTime());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // Check ownership or admin
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to modify this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cancelled bookings cannot be modified");
        }

        Resource resource = request.getResourceId() != null
                ? resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"))
                : booking.getResource();

        // Detect overlapping bookings excluding this booking
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                bookingId
        );

        if (!conflicts.isEmpty()) {
            Booking conflict = conflicts.get(0);
            throw new BookingConflictException("Conflict detected: Resource is already booked between " +
                    conflict.getStartTime().format(TIME_FORMATTER) + " and " +
                    conflict.getEndTime().format(TIME_FORMATTER));
        }

        booking.setResource(resource);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setRequestedServices(request.getRequestedServices());

        Booking updated = bookingRepository.save(booking);

        // Audit log
        auditLogService.logAction(user, "BOOKING_MODIFIED",
                "Booking #" + updated.getId() + " modified to " + updated.getStartTime().format(TIME_FORMATTER) + " - " + updated.getEndTime().format(TIME_FORMATTER));

        // Notification: Booking Modification Alert
        notificationService.sendNotification(
                booking.getUser(),
                "Booking Updated: " + resource.getName(),
                "Your reservation has been rescheduled to " +
                        updated.getStartTime().format(TIME_FORMATTER) + " - " + updated.getEndTime().format(TIME_FORMATTER) + ".",
                "REMINDER",
                "EMAIL_SMS"
        );

        return updated;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        // Audit Log
        auditLogService.logAction(user, "BOOKING_CANCELLED",
                "Booking #" + booking.getId() + " for " + booking.getResource().getName() + " was cancelled by " + user.getUsername());

        // Notification: Cancellation Alert
        notificationService.sendNotification(
                booking.getUser(),
                "Booking Cancelled: " + booking.getResource().getName(),
                "Your reservation for " + booking.getResource().getName() + " on " +
                        booking.getStartTime().format(TIME_FORMATTER) + " has been cancelled.",
                "CANCELLATION",
                "EMAIL_SMS"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public boolean checkConflict(Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(resourceId, start, end, excludeId);
        return !conflicts.isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByStartTimeDesc();
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUtilizationReport(LocalDateTime startRange, LocalDateTime endRange) {
        LocalDateTime start = startRange != null ? startRange : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endRange != null ? endRange : LocalDateTime.now().plusDays(30);

        List<Object[]> rawData = bookingRepository.getResourceUtilizationReport(start, end);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rawData) {
            Map<String, Object> map = new HashMap<>();
            map.put("resourceId", row[0]);
            map.put("resourceName", row[1]);
            map.put("resourceType", row[2]);
            map.put("totalBookings", row[3]);
            long totalMinutes = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            map.put("totalHours", Math.round((totalMinutes / 60.0) * 10.0) / 10.0);
            result.add(map);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> stats = new HashMap<>();
        long totalResources = resourceRepository.count();
        long activeBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long totalUsers = bookingRepository.findAll().stream().map(b -> b.getUser().getId()).distinct().count();

        stats.put("totalResources", totalResources);
        stats.put("activeBookings", activeBookings);
        stats.put("cancelledBookings", cancelledBookings);
        stats.put("activeUsers", totalUsers);

        return stats;
    }

    private void validateBookingTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start time and end time must not be null");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new BadRequestException("Start time must be before end time");
        }
        if (start.isBefore(LocalDateTime.now().minusMinutes(10))) {
            throw new BadRequestException("Cannot create booking in the past");
        }
    }
}
