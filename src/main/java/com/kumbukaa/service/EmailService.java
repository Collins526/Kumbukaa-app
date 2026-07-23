package com.kumbukaa.service;

import org.springframework.beans.factory.annotation.Value;
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
        String body = String.format("Your one-time password is %s. It expires in 10 minutes.", code);
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, "Your Kumbukaa OTP Code", body);
            return;
        }

        if (smtpFromEmail != null && !smtpFromEmail.isBlank()) {
            sendViaSmtp(to, "Your Kumbukaa OTP Code", body);
            return;
        }

        throw new MailSendException("No valid email provider configured.");
    }

    public void sendPasswordResetOtpEmail(String to, String userName, String code) {
        String name = (userName == null || userName.isBlank()) ? "there" : userName;
        String body = String.format("Hello %s,\n\nYou requested to reset your Kumbukaa account password.\n\nYour verification code is:\n\n%s\n\nThis code expires in 10 minutes.\n\nIf you did not request this password reset, please ignore this email.", name, code);

        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, "Password Reset Verification Code", body);
            return;
        }

        if (smtpFromEmail != null && !smtpFromEmail.isBlank()) {
            sendViaSmtp(to, "Password Reset Verification Code", body);
            return;
        }

        throw new MailSendException("No valid email provider configured.");
    }

    private void sendViaSmtp(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpFromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendViaResend(String to, String subject, String body) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new MailSendException("Resend API key is not configured.");
        }

        String payload = String.format("{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"text\":\"%s\"}",
                resendFromEmail, to, subject, body.replace("\n", "\\n"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
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
