package com.mostafa.eticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "CLOB")
    private String description;

    @NotBlank
    @Email
    @Column(nullable = false)
    private String requesterEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column
    private LocalDate dueDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}