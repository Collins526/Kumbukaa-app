package com.kumbukaa.util;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String normalized = phoneNumber.trim().replaceAll("[^0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
