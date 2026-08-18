package com.mostafa.eticket.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterViewerRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;
}