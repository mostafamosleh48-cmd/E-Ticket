package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Invitation;
import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.User;
import com.mostafa.eticket.dto.auth.AcceptInvitationRequest;
import com.mostafa.eticket.dto.auth.InvitationRequest;
import com.mostafa.eticket.dto.auth.InvitationResponse;
import com.mostafa.eticket.dto.auth.LoginRequest;
import com.mostafa.eticket.dto.auth.LoginResponse;
import com.mostafa.eticket.dto.auth.RegisterAgentRequest;
import com.mostafa.eticket.dto.auth.RegisterViewerRequest;
import com.mostafa.eticket.exception.DuplicateUserException;
import com.mostafa.eticket.exception.InvitationEmailException;
import com.mostafa.eticket.exception.InvalidInvitationException;
import com.mostafa.eticket.repository.InvitationRepository;
import com.mostafa.eticket.repository.OrganizationRepository;
import com.mostafa.eticket.repository.UserRepository;
import com.mostafa.eticket.security.AuthUser;
import com.mostafa.eticket.security.CustomUserDetails;
import com.mostafa.eticket.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private OrganizationRepository organizationRepository;
    private InvitationRepository invitationRepository;
    private InvitationEmailService invitationEmailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        invitationRepository = mock(InvitationRepository.class);
        invitationEmailService = mock(InvitationEmailService.class);
        authService = new AuthService(authenticationManager, jwtService, passwordEncoder,
                userRepository, organizationRepository, invitationRepository, invitationEmailService,
                Duration.ofHours(24));
    }

    private Organization org(Long id) {
        Organization org = new Organization();
        org.setId(id);
        org.setName("Acme");
        return org;
    }

    private User agentUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setRole(Role.AGENT);
        user.setOrganization(org(5L));
        return user;
    }

    private User viewerUser() {
        User user = new User();
        user.setId(10L);
        user.setUsername("viewer");
        user.setEmail("viewer@b.com");
        user.setRole(Role.VIEWER);
        return user;
    }

    private Invitation invitation(LocalDateTime expiresAt) {
        Invitation invitation = new Invitation();
        invitation.setUser(viewerUser());
        invitation.setOrganization(org(5L));
        invitation.setExpiresAt(expiresAt);
        return invitation;
    }

    private Authentication authenticatedPrincipal(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = agentUser();
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticatedPrincipal(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("alice", "secret"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getRole()).isEqualTo(Role.AGENT);
    }

    @Test
    void loginThrowsBadCredentialsForWrongPassword() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void registerAgentCreatesOrganizationAndUser() {
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(5L);
            return org;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = authService.registerAgent(
                new RegisterAgentRequest("alice", "alice@b.com", "secret123", "Acme"));

        verify(organizationRepository).save(argThat(o -> o.getName().equals("Acme")));
        verify(userRepository).save(argThat(u -> u.getRole() == Role.AGENT
                && u.getOrganization().getId() == 5L
                && u.getPasswordHash().equals("encoded-hash")));
        assertThat(response.getRole()).isEqualTo(Role.AGENT);
    }

    @Test
    void registerAgentRejectsDuplicateUsername() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.registerAgent(
                new RegisterAgentRequest("alice", "alice@b.com", "secret123", "Acme")))
                .isInstanceOf(DuplicateUserException.class);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void registerAgentRejectsDuplicateOrganizationName() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(organizationRepository.findByName("Acme")).thenReturn(Optional.of(new Organization()));

        assertThatThrownBy(() -> authService.registerAgent(
                new RegisterAgentRequest("alice", "alice@b.com", "secret123", "Acme")))
                .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void createInvitationStoresHashNotRawTokenAndLinksUser() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org(5L)));
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser()));

        InvitationResponse response = authService.createInvitation(
                new InvitationRequest("viewer"), new AuthUser("agent", Role.AGENT, 5L));

        assertThat(response.getUsername()).isEqualTo("viewer");
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(invitationRepository).save(argThat(inv ->
                inv.getTokenHash() != null
                        && inv.getTokenHash().length() == 64
                        && !inv.getTokenHash().equals(response.getToken())
                        && inv.getUser().getId() == 10L
                        && inv.getOrganization().getId() == 5L
                        && inv.getExpiresAt().isAfter(LocalDateTime.now())));
        verify(invitationEmailService).sendInvitation(eq("viewer@b.com"), eq(response.getToken()), any(LocalDateTime.class));
    }

    @Test
    void createInvitationRejectsNonAgent() {
        assertThatThrownBy(() -> authService.createInvitation(
                new InvitationRequest("viewer"), new AuthUser("viewer", Role.VIEWER, 5L)))
                .isInstanceOf(InvalidInvitationException.class);
        verifyNoInteractions(invitationRepository);
        verifyNoInteractions(invitationEmailService);
    }

    @Test
    void createInvitationRejectsUnknownViewer() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org(5L)));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.createInvitation(
                new InvitationRequest("nobody"), new AuthUser("agent", Role.AGENT, 5L)))
                .isInstanceOf(InvalidInvitationException.class);
        verifyNoInteractions(invitationRepository);
        verifyNoInteractions(invitationEmailService);
    }

    @Test
    void createInvitationRejectsNonViewerRole() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org(5L)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(agentUser()));

        assertThatThrownBy(() -> authService.createInvitation(
                new InvitationRequest("alice"), new AuthUser("agent", Role.AGENT, 5L)))
                .isInstanceOf(InvalidInvitationException.class);
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void createInvitationRejectsViewerAlreadyInOrganization() {
        User viewer = viewerUser();
        viewer.setOrganization(org(9L));
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org(5L)));
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> authService.createInvitation(
                new InvitationRequest("viewer"), new AuthUser("agent", Role.AGENT, 5L)))
                .isInstanceOf(InvalidInvitationException.class);
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void createInvitationPropagatesEmailFailure() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.of(org(5L)));
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser()));
        doThrow(new InvitationEmailException(
                "Failed to send invitation to viewer@b.com", new RuntimeException()))
                .when(invitationEmailService).sendInvitation(anyString(), anyString(), any(LocalDateTime.class));

        assertThatThrownBy(() -> authService.createInvitation(
                new InvitationRequest("viewer"), new AuthUser("agent", Role.AGENT, 5L)))
                .isInstanceOf(InvitationEmailException.class);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void registerViewerSelfCreatesPendingViewerWithNoOrganization() {
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.registerViewerSelf(new RegisterViewerRequest("viewer", "viewer@b.com", "secret123"));

        verify(userRepository).save(argThat(u -> u.getUsername().equals("viewer")
                && u.getEmail().equals("viewer@b.com")
                && u.getRole() == Role.VIEWER
                && u.getOrganization() == null
                && u.getPasswordHash().equals("encoded-hash")));
    }

    @Test
    void registerViewerSelfRejectsDuplicateUsername() {
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.registerViewerSelf(
                new RegisterViewerRequest("viewer", "viewer@b.com", "secret123")))
                .isInstanceOf(DuplicateUserException.class);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void acceptInvitationConsumesInvitationAssignsOrganizationAndReturnsFreshToken() {
        Invitation invitation = invitation(LocalDateTime.now().plusHours(1));
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));
        when(userRepository.findById(10L)).thenReturn(Optional.of(invitation.getUser()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(invitation.getUser())).thenReturn("fresh-jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.acceptInvitation(new AcceptInvitationRequest("some-token"));

        assertThat(invitation.getUsedAt()).isNotNull();
        assertThat(invitation.getUser().getOrganization()).isNotNull();
        assertThat(invitation.getUser().getOrganization().getId()).isEqualTo(5L);
        assertThat(response.getToken()).isEqualTo("fresh-jwt");
        verify(userRepository).save(argThat(u -> u.getOrganization() != null
                && u.getOrganization().getId() == 5L));
    }

    @Test
    void acceptInvitationRejectsUnknownToken() {
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.acceptInvitation(new AcceptInvitationRequest("nope")))
                .isInstanceOf(InvalidInvitationException.class);
    }

    @Test
    void acceptInvitationRejectsExpiredToken() {
        Invitation invitation = invitation(LocalDateTime.now().minusHours(1));
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> authService.acceptInvitation(new AcceptInvitationRequest("some-token")))
                .isInstanceOf(InvalidInvitationException.class);
        assertThat(invitation.getUsedAt()).isNull();
    }

    @Test
    void acceptInvitationRejectsUsedToken() {
        Invitation invitation = invitation(LocalDateTime.now().plusHours(1));
        invitation.setUsedAt(LocalDateTime.now());
        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> authService.acceptInvitation(new AcceptInvitationRequest("some-token")))
                .isInstanceOf(InvalidInvitationException.class);
    }
}