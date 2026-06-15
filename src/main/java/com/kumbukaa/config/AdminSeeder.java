package com.kumbukaa.config;

import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.admin.email:admin@localhost}")
    private String defaultAdminEmail;

    @Value("${app.admin.password:admin}")
    private String defaultAdminPassword;

    @Value("${app.admin.phone:0000000000}")
    private String defaultAdminPhone;

    public AdminSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @SuppressWarnings("null")
    public void run(ApplicationArguments args) throws Exception {
        try {
            logger.info("AdminSeeder: checking for existing ROLE_ADMIN users");
            boolean hasAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN"));

            if (hasAdmin) {
                logger.info("AdminSeeder: ROLE_ADMIN user already exists, skipping creation");
                return;
            }

            logger.info("AdminSeeder: no ROLE_ADMIN found, creating default admin with email: {}", defaultAdminEmail);

            User admin = User.builder()
                    .fullName("Administrator")
                    .email(defaultAdminEmail.toLowerCase().trim())
                    .phoneNumber(defaultAdminPhone)
                    .passwordHash(encoder.encode(defaultAdminPassword))
                    .roles("ROLE_ADMIN")
                    .mustChangePassword(true)
                    .build();

            User savedAdmin = userRepository.save(admin);
            logger.info("AdminSeeder: Default ADMIN account created successfully with ID: {}, email: {} (must change password on first login)", 
                    savedAdmin.getId(), defaultAdminEmail);
        } catch (Exception e) {
            logger.error("AdminSeeder: failed to create default admin account", e);
            throw e;
        }
    }
}
