package com.mostafa.eticket.security;

import com.mostafa.eticket.domain.Role;

public record AuthUser(String username, Role role, Long organizationId) {
}