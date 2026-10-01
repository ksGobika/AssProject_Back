package com.kiot.csrm.service;

import com.kiot.csrm.dto.RegisterRequest;
import com.kiot.csrm.entity.Role;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.entity.UserStatus;
import com.kiot.csrm.exception.BadRequestException;
import com.kiot.csrm.exception.ResourceNotFoundException;
import com.kiot.csrm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.STUDENT;
        if (assignedRole == Role.ADMIN) {
            throw new BadRequestException("Cannot register directly as an ADMIN");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFullName(request.getFullName().trim());
        user.setDepartment(request.getDepartment());
        user.setRole(assignedRole);
        user.setStatus(UserStatus.PENDING); // Requires admin approval as per requirements

        User savedUser = userRepository.save(user);

        // Audit log
        auditLogService.logAction(savedUser, "USER_REGISTERED",
                "New account registered: " + savedUser.getUsername() + " (" + savedUser.getRole() + ")");

        // Welcome notification
        notificationService.sendNotification(
                savedUser,
                "Account Registration Received",
                "Your registration for " + savedUser.getFullName() + " (" + savedUser.getRole() + ") is pending Admin approval.",
                "SYSTEM",
                "EMAIL_SMS"
        );

        return savedUser;
    }

    @Transactional
    public User approveUser(Long userId, User adminUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(UserStatus.APPROVED);
        User updated = userRepository.save(user);

        auditLogService.logAction(adminUser, "USER_APPROVED",
                "User approved: " + user.getUsername() + " (" + user.getRole() + ") by " + adminUser.getUsername());

        notificationService.sendNotification(
                user,
                "Account Approved!",
                "Congratulations! Your CSRM account has been approved by the Administrator. You can now log in and book resources.",
                "CONFIRMATION",
                "EMAIL_SMS"
        );

        return updated;
    }

    @Transactional
    public User rejectUser(Long userId, User adminUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(UserStatus.REJECTED);
        User updated = userRepository.save(user);

        auditLogService.logAction(adminUser, "USER_REJECTED",
                "User rejected: " + user.getUsername() + " (" + user.getRole() + ") by " + adminUser.getUsername());

        notificationService.sendNotification(
                user,
                "Account Application Update",
                "Your CSRM account registration could not be approved at this time. Please contact the campus admin office.",
                "CANCELLATION",
                "EMAIL_SMS"
        );

        return updated;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public User changeUserRole(Long userId, Role newRole, User adminUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        User updated = userRepository.save(user);

        auditLogService.logAction(adminUser, "USER_ROLE_CHANGED",
                "Role changed for " + user.getUsername() + " from " + oldRole + " to " + newRole);

        return updated;
    }
}
