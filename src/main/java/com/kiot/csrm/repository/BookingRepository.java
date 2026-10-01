package com.kiot.csrm.repository;

import com.kiot.csrm.entity.Booking;
import com.kiot.csrm.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    List<Booking> findAllByOrderByStartTimeDesc();

    List<Booking> findByResourceIdAndStatusNot(Long resourceId, BookingStatus status);

    // Overlapping booking query: Checks if any non-cancelled booking overlaps with [startTime, endTime]
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.status != com.kiot.csrm.entity.BookingStatus.CANCELLED " +
           "AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId) " +
           "AND (b.startTime < :endTime AND b.endTime > :startTime)")
    List<Booking> findOverlappingBookings(@Param("resourceId") Long resourceId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("excludeBookingId") Long excludeBookingId);

    // Utilization query: aggregate bookings and calculate total duration in minutes
    @Query("SELECT b.resource.id AS resourceId, b.resource.name AS resourceName, b.resource.type AS resourceType, " +
           "COUNT(b.id) AS totalBookings, " +
           "SUM(FUNCTION('TIMESTAMPDIFF', MINUTE, b.startTime, b.endTime)) AS totalMinutes " +
           "FROM Booking b " +
           "WHERE b.status != com.kiot.csrm.entity.BookingStatus.CANCELLED " +
           "AND b.startTime >= :startRange AND b.endTime <= :endRange " +
           "GROUP BY b.resource.id, b.resource.name, b.resource.type")
    List<Object[]> getResourceUtilizationReport(@Param("startRange") LocalDateTime startRange,
                                               @Param("endRange") LocalDateTime endRange);

    long countByStatus(BookingStatus status);

    List<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
