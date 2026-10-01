package com.kiot.csrm.config;

import com.kiot.csrm.entity.*;
import com.kiot.csrm.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ResourceRepository resourceRepository,
                           BookingRepository bookingRepository,
                           AuditLogRepository auditLogRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsersAndData();
    }

    private void seedUsersAndData() {
        // 1. Seed or update required accounts with exact requested credentials
        upsertUser("admin", "admin123", "admin@campus.edu", "Campus Administrator", "Campus Operations", Role.ADMIN, UserStatus.APPROVED);
        upsertUser("faculty", "faculty123456", "faculty@campus.edu", "Faculty Member", "Computer Science", Role.FACULTY, UserStatus.APPROVED);
        upsertUser("student", "password123456", "student@campus.edu", "Campus Student", "Information Technology", Role.STUDENT, UserStatus.APPROVED);

        // Demo pending student for testing Admin account approval workflow
        upsertUser("priya.s", "password123", "priya@campus.edu", "Priya Sundar", "Mechanical Engineering", Role.STUDENT, UserStatus.PENDING);

        if (resourceRepository.count() == 0) {
            seedResourcesAndDemoData();
        }
    }

    private void upsertUser(String username, String rawPassword, String email, String fullName, String dept, Role role, UserStatus status) {
        User user = userRepository.findByUsername(username).orElse(new User());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setDepartment(dept);
        user.setRole(role);
        user.setStatus(status);
        userRepository.save(user);
    }

    private void seedResourcesAndDemoData() {
        User faculty = userRepository.findByUsername("faculty").orElse(null);
        User student = userRepository.findByUsername("student").orElse(null);
        User admin = userRepository.findByUsername("admin").orElse(null);

        // 2. Seed Resources
        Resource r1 = new Resource("Smart Lecture Hall A1", ResourceType.CLASSROOM, "Academic Block - 1st Floor", 60, true, "Equipped with 4K interactive projector, podium mic, and climate control.", 0.0);
        Resource r2 = new Resource("Executive Seminar Hall", ResourceType.CLASSROOM, "Administrative Wing - 2nd Floor", 120, true, "Auditorium acoustic panels, dual display, video conference setup.", 500.0);
        Resource r3 = new Resource("Advanced AI & Robotics Lab", ResourceType.LAB, "CS Complex - Ground Floor", 40, true, "Equipped with NVIDIA RTX Workstations and ROS robot kits.", 0.0);
        Resource r4 = new Resource("IoT & Embedded Systems Lab", ResourceType.LAB, "ECE Block - 3rd Floor", 35, true, "ESP32, Arduino boards, Raspberry Pi 5 clusters, logic analyzers.", 0.0);
        Resource r5 = new Resource("Digital Locker Bay A-01", ResourceType.LOCKER, "Library Plaza - Level 1", 1, true, "RFID & PIN secured storage locker with device charging port.", 20.0);
        Resource r6 = new Resource("Research Locker Bay B-08", ResourceType.LOCKER, "PG Research Wing - Ground Floor", 1, true, "Heavy-duty climate-controlled storage for instruments & notebooks.", 25.0);
        Resource r7 = new Resource("Mixed-Signal Storage Oscilloscope #4", ResourceType.EQUIPMENT, "Hardware Innovation Lab", 1, true, "Keysight 200MHz 4-Channel DSO with high-speed digital probes.", 150.0);
        Resource r8 = new Resource("4K 3D VR Headset Kit #2", ResourceType.EQUIPMENT, "Media & Simulation Lab", 1, true, "Meta Quest Pro with spatial controllers & wireless casting module.", 200.0);

        resourceRepository.saveAll(Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8));

        // 3. Seed Sample Bookings
        LocalDateTime now = LocalDateTime.now();

        if (faculty != null && student != null) {
            Booking b1 = new Booking(
                    faculty,
                    r1,
                    now.plusDays(1).withHour(10).withMinute(0).withSecond(0),
                    now.plusDays(1).withHour(12).withMinute(0).withSecond(0),
                    BookingStatus.CONFIRMED,
                    "Machine Learning Theory Lecture",
                    "Podium Mic, Projector Calibration"
            );

            Booking b2 = new Booking(
                    student,
                    r3,
                    now.plusDays(2).withHour(14).withMinute(0).withSecond(0),
                    now.plusDays(2).withHour(16).withMinute(30).withSecond(0),
                    BookingStatus.CONFIRMED,
                    "Robotics Club Hackathon Sprint",
                    "Lab Assistant Support"
            );

            bookingRepository.saveAll(Arrays.asList(b1, b2));
        }

        // 4. Seed Audit Logs
        if (admin != null) {
            AuditLog l1 = new AuditLog(admin, "SYSTEM_INIT", "System initialized with baseline campus resources and security configurations.");
            auditLogRepository.save(l1);
        }

        // 5. Seed Notifications
        if (faculty != null) {
            Notification n1 = new Notification(faculty, "Booking Confirmed: Smart Lecture Hall A1", "Your booking for Smart Lecture Hall A1 is confirmed for tomorrow 10:00 AM.", "CONFIRMATION", "EMAIL_SMS");
            notificationRepository.save(n1);
        }
        if (student != null) {
            Notification n2 = new Notification(student, "Booking Confirmed: Advanced AI Lab", "Your booking for Advanced AI & Robotics Lab is confirmed for 2:00 PM.", "CONFIRMATION", "EMAIL_SMS");
            notificationRepository.save(n2);
        }
    }
}
