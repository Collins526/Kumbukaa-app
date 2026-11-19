package com.kumbukaa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleUtilsTest {

    @Test
    void recognizesAdminRoleWithOrWithoutSpringPrefix() {
        assertTrue(UserRoleUtils.hasRole("ADMIN", "ROLE_ADMIN"));
        assertTrue(UserRoleUtils.hasRole("ROLE_ADMIN", "ADMIN"));
        assertTrue(UserRoleUtils.hasRole("ROLE_USER, admin", "ROLE_ADMIN"));
    }

    @Test
    void doesNotGrantAdminAccessToNonAdminRoles() {
        assertFalse(UserRoleUtils.hasRole("ROLE_USER", "ADMIN"));
        assertFalse(UserRoleUtils.hasRole("ROLE_SUPPORT_AGENT", "ADMIN"));
    }
}
