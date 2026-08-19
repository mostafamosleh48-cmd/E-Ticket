package com.mostafa.eticket.security;

import com.mostafa.eticket.domain.Role;

public record TokenClaims(String username, Role role, Long organizationId) {
}
