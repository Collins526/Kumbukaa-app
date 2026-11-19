package com.kumbukaa.config;

import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.UserRoleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
            logger.info("AdminSeeder: checking for existing default admin user");
            Optional<User> defaultAdminOpt = userRepository
                    .findByEmail(defaultAdminEmail.toLowerCase().trim());

            if (defaultAdminOpt.isPresent()) {
                User existingAdmin = defaultAdminOpt.get();
                boolean updateExistingAdmin = false;
                if (!UserRoleUtils.hasRole(existingAdmin.getRoles(), "ADMIN")) {
                    existingAdmin.setRoles("ROLE_ADMIN,ROLE_USER");
                    updateExistingAdmin = true;
                } else if (!UserRoleUtils.hasRole(existingAdmin.getRoles(), "USER")) {
                    existingAdmin.setRoles(existingAdmin.getRoles() + ",ROLE_USER");
                    updateExistingAdmin = true;
                }
                if (!"ACTIVE".equals(existingAdmin.getAccountStatus())) {
                    existingAdmin.setAccountStatus("ACTIVE");
                    updateExistingAdmin = true;
                }
                if (!Boolean.TRUE.equals(existingAdmin.getMustChangePassword())
                        && encoder.matches(defaultAdminPassword, existingAdmin.getPasswordHash())) {
                    existingAdmin.setMustChangePassword(true);
                    updateExistingAdmin = true;
                }
                if (updateExistingAdmin) userRepository.save(existingAdmin);
                logger.info("AdminSeeder: Default admin user already exists; verified full system permissions.");
            } else {
                logger.info("AdminSeeder: no ROLE_ADMIN found, creating default admin with email: {}", defaultAdminEmail);

                User admin = User.builder()
                        .fullName("Administrator")
                        .email(defaultAdminEmail.toLowerCase().trim())
                        .phoneNumber(defaultAdminPhone)
                        .passwordHash(encoder.encode(defaultAdminPassword))
                        .roles("ROLE_ADMIN,ROLE_USER")
                        .accountStatus("ACTIVE")
                        .mustChangePassword(true)
                        .build();

                User savedAdmin = userRepository.save(admin);
                logger.info(
                        "AdminSeeder: Default ADMIN account created successfully with ID: {}, email: {} (must change password on first login)",
                        savedAdmin.getId(), defaultAdminEmail);
            }

        } catch (Exception e) {
            logger.error("AdminSeeder: failed to create or verify default admin account", e);
            throw e;
        }
    }
}
