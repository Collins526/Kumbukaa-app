package com.kumbukaa.service;

import com.kumbukaa.entity.Lender;
import com.kumbukaa.repository.LenderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class LenderService {

    @Autowired
    private LenderRepository lenderRepository;

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
