package com.mostafa.eticket.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private String token;
    private String email;
    private LocalDateTime expiresAt;
}