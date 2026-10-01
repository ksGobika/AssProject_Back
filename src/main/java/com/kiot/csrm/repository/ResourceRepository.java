package com.kiot.csrm.repository;

import com.kiot.csrm.entity.Resource;
import com.kiot.csrm.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByAvailabilityTrue();

    List<Resource> findByType(ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.availability = true " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND r.id NOT IN (" +
           "    SELECT b.resource.id FROM Booking b " +
           "    WHERE b.status != com.kiot.csrm.entity.BookingStatus.CANCELLED " +
           "    AND (:startTime IS NOT NULL AND :endTime IS NOT NULL AND b.startTime < :endTime AND b.endTime > :startTime)" +
           ")")
    List<Resource> findAvailableResources(@Param("type") ResourceType type,
                                          @Param("search") String search,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
