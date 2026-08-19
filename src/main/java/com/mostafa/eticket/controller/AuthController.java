package com.mostafa.eticket.controller;

import com.mostafa.eticket.dto.auth.AcceptInvitationRequest;
import com.mostafa.eticket.dto.auth.InvitationRequest;
import com.mostafa.eticket.dto.auth.InvitationResponse;
import com.mostafa.eticket.dto.auth.LoginRequest;
import com.mostafa.eticket.dto.auth.LoginResponse;
import com.mostafa.eticket.dto.auth.RegisterAgentRequest;
import com.mostafa.eticket.dto.auth.RegisterViewerRequest;
import com.mostafa.eticket.security.SecurityUtils;
import com.mostafa.eticket.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register, login, and viewer invitation APIs")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register an agent",
            description = "Creates an organization and its first agent in one step, then returns a JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agent and organization created"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "Username or organization name already taken")
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registerAgent(
            @Valid @RequestBody RegisterAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAgent(request));
    }

    @Operation(summary = "Log in", description = "Authenticates a user and returns a JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid login data"),
        @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Invite a viewer",
            description = "Agent-only. Creates a single-use, expiring invitation for an existing viewer, "
                    + "identified by username, and emails them the token.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation created"),
        @ApiResponse(responseCode = "400", description = "Only agents can invite, no such viewer, "
                + "viewer already in an organization, or invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/invitations")
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody InvitationRequest request) {
        return ResponseEntity.ok(authService.createInvitation(request, SecurityUtils.currentUser()));
    }

    @Operation(
            summary = "Register a viewer",
            description = "Public self-registration. Creates a read-only viewer account with no organization; "
                    + "they see no tickets until an agent invites them and they accept.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Viewer created"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "Username already taken")
    })
    @PostMapping("/register/viewer")
    public ResponseEntity<LoginResponse> registerViewer(
            @Valid @RequestBody RegisterViewerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerViewerSelf(request));
    }

    @Operation(
            summary = "Accept an invitation",
            description = "Public, token-only. Redeems an invitation token and joins the inviting agent's "
                    + "organization, then returns a fresh JWT carrying the organization claim.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation accepted, new JWT returned"),
        @ApiResponse(responseCode = "400", description = "Missing, expired, or already-used invitation")
    })
    @PostMapping("/invitations/accept")
    public ResponseEntity<LoginResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(authService.acceptInvitation(request));
    }
}