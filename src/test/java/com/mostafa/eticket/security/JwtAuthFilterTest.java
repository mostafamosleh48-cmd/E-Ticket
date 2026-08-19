package com.mostafa.eticket.security;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "test-only-secret-not-for-production";

    private JwtService jwtService;
    private JwtAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600);
        filter = new JwtAuthFilter(jwtService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User agentUser() {
        Organization org = new Organization();
        org.setId(7L);
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setRole(Role.AGENT);
        user.setOrganization(org);
        return user;
    }

    private void runFilter(String authorizationHeader) throws Exception {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        filter.doFilter(request, response, chain);
    }

    @Test
    void validTokenPopulatesSecurityContext() throws Exception {
        String token = jwtService.generateToken(agentUser());

        runFilter("Bearer " + token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthUser.class);
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.role()).isEqualTo(Role.AGENT);
        assertThat(principal.organizationId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AGENT");
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingHeaderPassesThroughUnauthenticated() throws Exception {
        runFilter(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedHeaderPassesThroughUnauthenticated() throws Exception {
        runFilter("Basic abc123");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenPassesThroughUnauthenticated() throws Exception {
        runFilter("Bearer not-a-real-token");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void expiredTokenPassesThroughUnauthenticated() throws Exception {
        JwtService expired = new JwtService(SECRET, -1);
        JwtAuthFilter expiredFilter = new JwtAuthFilter(expired);
        String token = expired.generateToken(agentUser());
        when(request.getHeader(anyString())).thenReturn("Bearer " + token);

        expiredFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}