package com.kumbukaa.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class UserRoleUtils {

    private UserRoleUtils() {
    }

    public static String normalize(String role) {
        if (role == null || role.isBlank()) return null;
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    public static boolean hasRole(String roles, String role) {
        String expected = normalize(role);
        return expected != null && roles != null && Arrays.stream(roles.split(","))
                .map(UserRoleUtils::normalize)
                .anyMatch(expected::equals);
    }

    public static List<GrantedAuthority> authorities(String roles) {
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.stream(roles.split(","))
                .map(UserRoleUtils::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static boolean hasAuthority(Authentication authentication, String role) {
        String expected = normalize(role);
        return expected != null && authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(UserRoleUtils::normalize)
                .anyMatch(expected::equals);
    }
}
