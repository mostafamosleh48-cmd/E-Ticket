package com.mostafa.eticket.dto;

import com.mostafa.eticket.domain.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {

    @NotNull
    private Status status;
}