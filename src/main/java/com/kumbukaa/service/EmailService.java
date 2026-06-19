package com.kumbukaa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String smtpFromEmail;
    private final String resendApiKey;
    private final String resendFromEmail;
    private final HttpClient httpClient;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String smtpFromEmail,
                        @Value("${app.resend.api-key:}") String resendApiKey,
                        @Value("${app.resend.from-email:onboarding@resend.dev}") String resendFromEmail) {
        this.mailSender = mailSender;
        this.smtpFromEmail = smtpFromEmail;
        this.resendApiKey = resendApiKey;
        this.resendFromEmail = resendFromEmail;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendOtpEmail(String to, String code) {
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, code);
            return;
        }

        if (smtpFromEmail != null && !smtpFromEmail.isBlank()) {
            sendViaSmtp(to, code);
            return;
        }

        throw new MailSendException("No valid email provider configured.");
    }

    private void sendViaSmtp(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpFromEmail);
        message.setTo(to);
        message.setSubject("Your Kumbukaa OTP Code");
        message.setText(String.format("Your one-time password is %s. It expires in 10 minutes.", code));
        mailSender.send(message);
    }

    private void sendViaResend(String to, String code) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new MailSendException("Resend API key is not configured.");
        }

        String body = String.format("{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"Your Kumbukaa OTP Code\",\"text\":\"Your one-time password is %s. It expires in 10 minutes.\"}",
                resendFromEmail, to, code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new MailSendException("Resend email failed with status " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Failed to send email through Resend.", exception);
        }
    }
}
