package com.kumbukaa.service;

import com.kumbukaa.dto.LenderDTO;
import com.kumbukaa.entity.Lender;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.LenderRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
public class LenderService {

    @Autowired
    private LenderRepository lenderRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Lender createLenderFromDTO(LenderDTO lenderDTO) throws Exception {
        if (lenderDTO == null) {
            throw new IllegalArgumentException("Lender data cannot be null");
        }
        Long userId = Objects.requireNonNull(lenderDTO.getUserId(), "userId is required");

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new Exception("User with ID " + userId + " not found");
        }

        User user = userOptional.get();
        
        // Check if lender already exists for this user
        Optional<Lender> existingLender = lenderRepository.findByUserId(userId);
        Lender lender;
        if (existingLender.isPresent()) {
            lender = existingLender.get();
        } else {
            lender = new Lender();
            lender.setUser(user);
        }

        if (lenderDTO.getLenderName() == null || lenderDTO.getLenderName().trim().isEmpty()) {
            if (lender.getLenderName() == null || lender.getLenderName().trim().isEmpty()) {
                lender.setLenderName(user.getName());
            }
        } else {
            lender.setLenderName(lenderDTO.getLenderName());
        }

        if (lenderDTO.getAvailableCapital() != null) {
            lender.setAvailableCapital(lenderDTO.getAvailableCapital());
        }
        if (lenderDTO.getInterestRate() != null) {
            lender.setInterestRate(lenderDTO.getInterestRate());
        }
        if (lenderDTO.getDateOfBirth() != null) {
            lender.setDateOfBirth(lenderDTO.getDateOfBirth());
        }
        if (lenderDTO.getAddress() != null) {
            lender.setAddress(lenderDTO.getAddress());
        }
        if (lenderDTO.getCity() != null) {
            lender.setCity(lenderDTO.getCity());
        }
        if (lenderDTO.getPostalCode() != null) {
            lender.setPostalCode(lenderDTO.getPostalCode());
        }

        if (lenderDTO.getIsVerified() != null) {
            lender.setIsVerified(lenderDTO.getIsVerified());
        } else if (!existingLender.isPresent()) {
            lender.setIsVerified(false);
        }

        return lenderRepository.save(lender);
    }

    public Lender createLender(Lender lender) {
        if (lender == null) throw new IllegalArgumentException("Lender cannot be null");
        return lenderRepository.save(lender);
    }

    public Optional<Lender> findById(Long id) {
        if (id == null) return Optional.empty();
        return lenderRepository.findById(id);
    }

    public Optional<Lender> findByUserId(Long userId) {
        return lenderRepository.findByUserId(userId);
    }

    public List<Lender> findAll() {
        return lenderRepository.findAll();
    }

    public Lender updateLender(Lender lender) {
        if (lender == null) throw new IllegalArgumentException("Lender cannot be null");
        return lenderRepository.save(lender);
    }

    public void deleteLender(Long id) {
        if (id != null) {
            lenderRepository.deleteById(id);
        }
    }

    public void updateLentAmount(Long lenderId, Double amount) {
        if (lenderId == null) return;
        Optional<Lender> lender = lenderRepository.findById(lenderId);
        if (lender.isPresent()) {
            Lender l = lender.get();
            l.setTotalLent(l.getTotalLent() + amount);
            lenderRepository.save(l);
        }
    }

    public void updateRecoveredAmount(Long lenderId, Double amount) {
        if (lenderId == null) return;
        Optional<Lender> lender = lenderRepository.findById(lenderId);
        if (lender.isPresent()) {
            Lender l = lender.get();
            l.setTotalRecovered(l.getTotalRecovered() + amount);
            lenderRepository.save(l);
        }
    }
}
