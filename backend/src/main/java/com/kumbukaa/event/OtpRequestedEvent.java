package com.kumbukaa.event;

public class OtpRequestedEvent {

    private final String email;
    private final String code;

    public OtpRequestedEvent(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
