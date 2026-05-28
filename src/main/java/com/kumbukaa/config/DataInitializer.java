package com.kumbukaa.config;

import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.repository.AuthRepository;
import com.kumbukaa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class DataInitializer {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthRepository authRepository;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            if (authRepository.count() == 0) {
                seedUsers(5);
            } else {
                System.out.println("Database already seeded. Skipping initial data population.");
            }
        };
    }

    private void seedUsers(int count) {
        Random random = new Random();
        String[] firstNames = { "collo", "Jane", "Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George",
                "Hannah", "Ivan", "Julia", "Kevin", "Laura", "Mike", "Nina", "Oscar", "Paula", "Quincy", "Rose", "Sam",
                "Tina", "Ursula", "Victor", "Wendy", "Xander", "Yara", "Zack" };
        String[] lastNames = { "Doe", "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
                "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor",
                "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark" };

        for (int i = 1; i <= count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String emailLocalPart = (firstName + lastName + i).toLowerCase();
            String email = emailLocalPart + "@kumbukaa.com";

            // Skip if already exists
            if (authRepository.findByEmail(email).isPresent()) {
                System.out.println("User " + email + " already exists, skipping...");
                continue;
            }

            RegisterRequest request = new RegisterRequest();
            request.setName(firstName + " " + lastName);
            request.setEmail(email);
            request.setPhoneNumber("+2547" + String.format("%08d", random.nextInt(100000000)));
            request.setPassword("Password@123");
            request.setConfirmPassword("Password@123");

            try {
                authService.register(request);
                System.out.println("Seeded user " + i + "/" + count + ": " + email);
            } catch (Exception e) {
                System.err.println("Failed to seed user " + email + ": " + e.getMessage());
            }
        }

    }
}