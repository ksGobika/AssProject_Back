package com.kiot.csrm.service;

import com.kiot.csrm.dto.ResourceRequest;
import com.kiot.csrm.entity.Resource;
import com.kiot.csrm.entity.ResourceType;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.exception.ResourceNotFoundException;
import com.kiot.csrm.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AuditLogService auditLogService;

    public ResourceService(ResourceRepository resourceRepository, AuditLogService auditLogService) {
        this.resourceRepository = resourceRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Resource createResource(ResourceRequest request, User adminUser) {
        Resource resource = new Resource();
        resource.setName(request.getName().trim());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation().trim());
        resource.setCapacity(request.getCapacity() != null ? request.getCapacity() : 1);
        resource.setAvailability(request.getAvailability() != null ? request.getAvailability() : true);
        resource.setDescription(request.getDescription());
        resource.setHourlyRate(request.getHourlyRate() != null ? request.getHourlyRate() : 0.0);

        Resource saved = resourceRepository.save(resource);

        if (adminUser != null) {
            auditLogService.logAction(adminUser, "RESOURCE_CREATED",
                    "Resource created: " + saved.getName() + " (" + saved.getType() + ") at " + saved.getLocation());
        }

        return saved;
    }

    @Transactional
    public Resource updateResource(Long id, ResourceRequest request, User adminUser) {
        Resource resource = getResourceById(id);

        resource.setName(request.getName().trim());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation().trim());
        resource.setCapacity(request.getCapacity());
        resource.setAvailability(request.getAvailability());
        resource.setDescription(request.getDescription());
        resource.setHourlyRate(request.getHourlyRate());

        Resource updated = resourceRepository.save(resource);

        if (adminUser != null) {
            auditLogService.logAction(adminUser, "RESOURCE_UPDATED",
                    "Resource updated: ID " + id + " - " + updated.getName());
        }

        return updated;
    }

    @Transactional
    public void deleteResource(Long id, User adminUser) {
        Resource resource = getResourceById(id);
        String name = resource.getName();
        resourceRepository.delete(resource);

        if (adminUser != null) {
            auditLogService.logAction(adminUser, "RESOURCE_DELETED",
                    "Resource deleted: " + name + " (ID: " + id + ")");
        }
    }

    @Transactional(readOnly = true)
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Resource> getAvailableResources(ResourceType type, String search, LocalDateTime startTime, LocalDateTime endTime) {
        return resourceRepository.findAvailableResources(type, search, startTime, endTime);
    }
}
