package com.kiot.csrm.controller;

import com.kiot.csrm.dto.ApiResponse;
import com.kiot.csrm.entity.Resource;
import com.kiot.csrm.entity.ResourceType;
import com.kiot.csrm.service.ResourceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Resource>>> getAllResources() {
        return ResponseEntity.ok(ApiResponse.ok("Resources fetched", resourceService.getAllResources()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Resource>> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Resource details fetched", resourceService.getResourceById(id)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Resource>>> getAvailableResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<Resource> available = resourceService.getAvailableResources(type, search, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.ok("Available resources retrieved", available));
    }
}
