package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Invitation;
import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.User;
import com.mostafa.eticket.dto.auth.InvitationRequest;
import com.mostafa.eticket.dto.auth.InvitationResponse;
import com.mostafa.eticket.dto.auth.LoginRequest;
import com.mostafa.eticket.dto.auth.LoginResponse;
import com.mostafa.eticket.dto.auth.RegisterAgentRequest;
import com.mostafa.eticket.dto.auth.RegisterViewerRequest;
import com.mostafa.eticket.exception.DuplicateUserException;
import com.mostafa.eticket.exception.InvalidInvitationException;
import com.mostafa.eticket.repository.InvitationRepository;
import com.mostafa.eticket.repository.OrganizationRepository;
import com.mostafa.eticket.repository.UserRepository;
import com.mostafa.eticket.security.AuthUser;
import com.mostafa.eticket.security.CustomUserDetails;
import com.mostafa.eticket.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationEmailService invitationEmailService;
    private final Duration invitationDuration;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       InvitationRepository invitationRepository,
                       InvitationEmailService invitationEmailService,
                       @Value("${eticket.invitation.expiration}") Duration invitationDuration) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.invitationRepository = invitationRepository;
        this.invitationEmailService = invitationEmailService;
        this.invitationDuration = invitationDuration;
    }

    @Transactional
    public LoginResponse registerAgent(RegisterAgentRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUserException("Username already taken: " + request.getUsername());
        }
        if (organizationRepository.findByName(request.getOrganizationName()).isPresent()) {
            throw new DuplicateUserException("Organization name already taken: " + request.getOrganizationName());
        }

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization = organizationRepository.save(organization);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.AGENT);
        user.setOrganization(organization);
        user = userRepository.save(user);

        return loginResponseFor(user);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return loginResponseFor(principal.getUser());
    }

    @Transactional
    public InvitationResponse createInvitation(InvitationRequest request, AuthUser caller) {
        if (caller.role() != Role.AGENT) {
            throw new InvalidInvitationException("Only agents can create invitations");
        }
        Organization organization = organizationRepository.findById(caller.organizationId())
                .orElseThrow(() -> new InvalidInvitationException("Organization not found"));

        String rawToken = generateInvitationToken();

        Invitation invitation = new Invitation();
        invitation.setTokenHash(hashToken(rawToken));
        invitation.setEmail(request.getEmail());
        invitation.setOrganization(organization);
        invitation.setExpiresAt(LocalDateTime.now().plus(invitationDuration));
        invitationRepository.save(invitation);
        invitationEmailService.sendInvitation(request.getEmail(), rawToken, invitation.getExpiresAt());

        return new InvitationResponse(rawToken, request.getEmail(), invitation.getExpiresAt());
    }

    @Transactional
    public LoginResponse registerViewer(RegisterViewerRequest request) {
        Invitation invitation = invitationRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new InvalidInvitationException("Invitation not found"));
        if (invitation.getUsedAt() != null) {
            throw new InvalidInvitationException("Invitation has already been used");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInvitationException("Invitation has expired");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUserException("Username already taken: " + request.getUsername());
        }

        invitation.setUsedAt(LocalDateTime.now());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(invitation.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.VIEWER);
        user.setOrganization(invitation.getOrganization());
        user = userRepository.save(user);

        return loginResponseFor(user);
    }

    private LoginResponse loginResponseFor(User user) {
        return new LoginResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationSeconds(), user.getRole());
    }

    private String generateInvitationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}