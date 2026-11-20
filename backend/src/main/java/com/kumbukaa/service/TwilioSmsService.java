package com.kumbukaa.service;

import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class TwilioSmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String defaultCountryCode;
    private final HttpClient httpClient;

    @Autowired
    public TwilioSmsService(
            @Value("${app.twilio.account-sid:}") String accountSid,
            @Value("${app.twilio.auth-token:}") String authToken,
            @Value("${app.twilio.from-number:}") String fromNumber,
            @Value("${app.twilio.default-country-code:254}") String defaultCountryCode) {
        this(accountSid, authToken, fromNumber, defaultCountryCode, HttpClient.newHttpClient());
    }

    TwilioSmsService(
            String accountSid,
            String authToken,
            String fromNumber,
            String defaultCountryCode,
            HttpClient httpClient) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.defaultCountryCode = defaultCountryCode;
        this.httpClient = httpClient;
    }

    public void sendPasswordResetOtpSms(String phoneNumber, String code) {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
            throw new IllegalStateException(
                    "Phone verification is unavailable because Twilio is not configured.");
        }

        String recipient = toE164(phoneNumber);
        String form = "To=" + encode(recipient)
                + "&From=" + encode(fromNumber)
                + "&Body=" + encode("Your Kumbukaa password reset code is " + code
                + ". It expires in 10 minutes.");
        String credentials = Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/"
                        + accountSid + "/Messages.json"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Could not send the verification SMS. Please try again later.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not send the verification SMS. Please try again later.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Could not send the verification SMS. Please try again later.", exception);
        }
    }

    static String toE164(String phoneNumber, String defaultCountryCode) {
        String digits = PhoneNumberUtils.normalize(phoneNumber);
        String countryCode = PhoneNumberUtils.normalize(defaultCountryCode);
        if (digits == null || countryCode == null) {
            throw new IllegalArgumentException("A valid phone number is required");
        }

        if (digits.startsWith("0")) {
            digits = countryCode + digits.substring(1);
        } else if (digits.length() <= 10 && !digits.startsWith(countryCode)) {
            digits = countryCode + digits;
        }
        if (digits.length() < 8 || digits.length() > 15) {
            throw new IllegalArgumentException("A valid phone number is required");
        }
        return "+" + digits;
    }

    private String toE164(String phoneNumber) {
        return toE164(phoneNumber, defaultCountryCode);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
