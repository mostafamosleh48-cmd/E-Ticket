package com.mostafa.eticket.security;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.User;
import com.mostafa.eticket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    private User agentUser() {
        Organization org = new Organization();
        org.setId(7L);
        org.setName("Acme");

        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setRole(Role.AGENT);
        user.setOrganization(org);
        return user;
    }

    @Test
    void loadUserByUsernameReturnsAuthoritiesAndOrganization() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(agentUser()));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AGENT");

        CustomUserDetails custom = (CustomUserDetails) details;
        assertThat(custom.getRole()).isEqualTo(Role.AGENT);
        assertThat(custom.getOrganizationId()).isEqualTo(7L);
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}