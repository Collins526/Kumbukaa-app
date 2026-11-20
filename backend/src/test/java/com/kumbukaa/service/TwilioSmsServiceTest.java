package com.kumbukaa.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwilioSmsServiceTest {

    @Test
    void sendPasswordResetOtpSmsPostsToTwilioWithBasicAuth() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock();
        when(response.statusCode()).thenReturn(201);
        when(httpClient.<String>send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        TwilioSmsService service = new TwilioSmsService(
                "AC123", "test-token", "+15005550006", "254", httpClient);

        service.sendPasswordResetOtpSms("0712345678", "123456");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<String>send(
                request.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals(
                "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json",
                request.getValue().uri().toString());
        assertEquals("Basic", request.getValue().headers()
                .firstValue("Authorization").orElseThrow().split(" ")[0]);
        assertEquals("application/x-www-form-urlencoded",
                request.getValue().headers().firstValue("Content-Type").orElseThrow());
    }

    @Test
    void sendPasswordResetOtpSmsFailsClearlyWhenTwilioIsNotConfigured() {
        TwilioSmsService service = new TwilioSmsService(
                "", "", "", "254", HttpClient.newHttpClient());

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.sendPasswordResetOtpSms("0712345678", "123456"));

        assertEquals(
                "Phone verification is unavailable because Twilio is not configured.",
                error.getMessage());
    }

    @Test
    void toE164FormatsKenyanLocalAndInternationalNumbers() {
        assertEquals("+254712345678", TwilioSmsService.toE164("0712 345 678", "254"));
        assertEquals("+254712345678", TwilioSmsService.toE164("+254 712 345 678", "254"));
        assertEquals("+14155550123", TwilioSmsService.toE164("+14155550123", "254"));
    }
}
