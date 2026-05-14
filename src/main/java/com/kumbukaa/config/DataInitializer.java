package com.kumbukaa.config;

import com.kumbukaa.dto.BorrowerDTO;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.entity.Auth;
import com.kumbukaa.entity.Lender;
import com.kumbukaa.entity.User;
import com.kumbukaa.enums.Role;
import com.kumbukaa.repository.AuthRepository;
import com.kumbukaa.service.AuthService;
import com.kumbukaa.service.BorrowerService;
import com.kumbukaa.service.LenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Random;

@Configuration
public class DataInitializer {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private LenderService lenderService;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            if (authRepository.count() == 0) {
                seedUsers(5);
            } else {
                System.out.println("Database already seeded. Skipping initial data population.");
            }
            repairMissingRoleRecords();
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

            // Alternating roles
            Role role = i % 2 == 0 ? Role.BORROWER : Role.LENDER;
            request.setRole(role.toString());

            try {
                authService.register(request);
                Auth savedAuth = authRepository.findByEmail(email).orElseThrow();
                User savedUser = savedAuth.getUser();

                if (role == Role.BORROWER) {
                    BorrowerDTO borrowerDTO = new BorrowerDTO();
                    borrowerDTO.setUserId(savedUser.getId());
                    borrowerDTO.setCreditScore(600 + random.nextInt(201));
                    borrowerDTO.setIdNumber("ID" + String.format("%08d", random.nextInt(100000000)));
                    borrowerDTO.setEmploymentStatus(randomEmploymentStatus(random));
                    borrowerDTO.setAnnualIncome(20000.0 + random.nextDouble() * 80000.0);
                    borrowerDTO.setDateOfBirth(randomDate(random));
                    borrowerDTO.setAddress(random.nextInt(9999) + " " + lastName + " St");
                    borrowerDTO.setCity(randomCity(random));
                    borrowerDTO.setPostalCode(String.format("%05d", random.nextInt(100000)));
                    borrowerDTO.setIsVerified(true);
                    borrowerDTO.setTotalBorrowed(0.0);
                    borrowerDTO.setTotalRepaid(0.0);
                    borrowerService.createBorrower(borrowerDTO);
                } else {
                    Lender lender = new Lender();
                    lender.setUser(savedUser);
                    lender.setLenderName(request.getName());
                    lender.setAvailableCapital(50000.0 + random.nextDouble() * 150000.0);
                    lender.setInterestRate(5.0 + random.nextDouble() * 10.0);
                    lender.setDateOfBirth(randomDate(random));
                    lender.setAddress(random.nextInt(9999) + " " + lastName + " Ave");
                    lender.setCity(randomCity(random));
                    lender.setPostalCode(String.format("%05d", random.nextInt(100000)));
                    lender.setIsVerified(true);
                    lender.setTotalLent(0.0);
                    lender.setTotalRecovered(0.0);
                    lenderService.createLender(lender);
                }

                System.out.println("Seeded user " + i + "/" + count + ": " + email + " (" + role + ")");
            } catch (Exception e) {
                System.err.println("Failed to seed user " + email + ": " + e.getMessage());
            }
        }

    }

    private String randomEmploymentStatus(Random random) {
        String[] statuses = {"EMPLOYED", "SELF_EMPLOYED", "UNEMPLOYED", "FREELANCER"};
        return statuses[random.nextInt(statuses.length)];
    }

    private String randomCity(Random random) {
        String[] cities = {"Nairobi", "Mombasa", "Kisumu", "Nakuru", "Eldoret", "Thika"};
        return cities[random.nextInt(cities.length)];
    }

    private java.time.LocalDate randomDate(Random random) {
        int year = 1980 + random.nextInt(25);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return LocalDate.of(year, month, day);
    }

    private void repairMissingRoleRecords() {
        authRepository.findAll().forEach(auth -> {
            User user = auth.getUser();
            if (user == null) {
                return;
            }
            if (user.getRole() == Role.BORROWER) {
                if (borrowerService.findByUserId(user.getId()).isEmpty()) {
                    BorrowerDTO borrowerDTO = new BorrowerDTO();
                    borrowerDTO.setUserId(user.getId());
                    borrowerDTO.setCreditScore(600);
                    borrowerDTO.setIdNumber("ID-" + user.getId());
                    borrowerDTO.setEmploymentStatus("EMPLOYED");
                    borrowerDTO.setAnnualIncome(0.0);
                    borrowerDTO.setDateOfBirth(LocalDate.of(1990, 1, 1));
                    borrowerDTO.setAddress("Unknown");
                    borrowerDTO.setCity("Unknown");
                    borrowerDTO.setPostalCode("00000");
                    borrowerDTO.setIsVerified(false);
                    borrowerDTO.setTotalBorrowed(0.0);
                    borrowerDTO.setTotalRepaid(0.0);
                    borrowerService.createBorrower(borrowerDTO);
                    System.out.println("Created missing borrower record for user " + user.getEmail());
                }
            } else if (user.getRole() == Role.LENDER) {
                if (lenderService.findByUserId(user.getId()).isEmpty()) {
                    Lender lender = new Lender();
                    lender.setUser(user);
                    lender.setLenderName(user.getName());
                    lender.setAvailableCapital(0.0);
                    lender.setInterestRate(5.0);
                    lender.setDateOfBirth(LocalDate.of(1990, 1, 1));
                    lender.setAddress("Unknown");
                    lender.setCity("Unknown");
                    lender.setPostalCode("00000");
                    lender.setIsVerified(false);
                    lender.setTotalLent(0.0);
                    lender.setTotalRecovered(0.0);
                    lenderService.createLender(lender);
                    System.out.println("Created missing lender record for user " + user.getEmail());
                }
            }
        });
    }
}