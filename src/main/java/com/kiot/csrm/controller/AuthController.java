package com.kiot.csrm.controller;

import com.kiot.csrm.dto.*;
import com.kiot.csrm.entity.User;
import com.kiot.csrm.entity.UserStatus;
import com.kiot.csrm.exception.BadRequestException;
import com.kiot.csrm.repository.UserRepository;
import com.kiot.csrm.security.JwtTokenProvider;
import com.kiot.csrm.service.AuditLogService;
import com.kiot.csrm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserService userService,
                          UserRepository userRepository,
                          AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = userService.registerUser(request);

        // If newly registered user is PENDING approval, inform them
        AuthResponse response = new AuthResponse(
                null,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getDepartment(),
                savedUser.getRole(),
                savedUser.getStatus()
        );

        return ResponseEntity.ok(ApiResponse.ok("Registration successful! Your account is pending Admin approval.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new BadRequestException("Your account is pending Admin approval. Please contact the administrator.");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new BadRequestException("Your account registration was rejected. Please contact the administrator.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        auditLogService.logAction(user, "LOGIN_SUCCESS", "User logged into CSRM portal");

        AuthResponse authResponse = new AuthResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus()
        );

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Not authenticated");
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        AuthResponse response = new AuthResponse(
                null,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                user.getRole(),
                user.getStatus()
        );

        return ResponseEntity.ok(ApiResponse.ok("Profile fetched successfully", response));
    }
}
