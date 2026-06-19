package com.kumbukaa.listener;

import com.kumbukaa.event.OtpRequestedEvent;
import com.kumbukaa.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OtpEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailListener.class);
    private final EmailService emailService;

    public OtpEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOtpRequested(OtpRequestedEvent event) {
        try {
            emailService.sendOtpEmail(event.getEmail(), event.getCode());
        } catch (Exception ex) {
            log.error("Failed to send OTP email after transaction commit", ex);
        }
    }
}
