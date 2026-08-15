package com.mostafa.eticket.mapper;

import com.mostafa.eticket.domain.Priority;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.PagedTicketResponse;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperTest {

    private TicketMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TicketMapper.class);
    }

    private CreateTicketRequest createRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Database is down");
        request.setDescription("Cannot connect to the primary database");
        request.setRequesterEmail("analyst@company.com");
        request.setPriority(Priority.CRITICAL);
        request.setDueDate(LocalDate.now().plusDays(1));
        return request;
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Database is down");
        ticket.setDescription("Cannot connect to the primary database");
        ticket.setRequesterEmail("analyst@company.com");
        ticket.setPriority(Priority.CRITICAL);
        ticket.setStatus(Status.IN_PROGRESS);
        ticket.setDueDate(LocalDate.of(2026, 8, 16));
        ticket.setCreatedAt(LocalDateTime.of(2026, 8, 15, 9, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 8, 15, 10, 30));
        return ticket;
    }

    @Test
    void mapsCreateRequestToEntityWithOpenStatusAndNoGeneratedFields() {
        Ticket entity = mapper.toEntity(createRequest());

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTitle()).isEqualTo("Database is down");
        assertThat(entity.getDescription()).isEqualTo("Cannot connect to the primary database");
        assertThat(entity.getRequesterEmail()).isEqualTo("analyst@company.com");
        assertThat(entity.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(entity.getStatus()).isEqualTo(Status.OPEN);
        assertThat(entity.getDueDate()).isEqualTo(createRequest().getDueDate());
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void mapsEntityToResponseWithAllFields() {
        TicketResponse response = mapper.toResponse(ticket());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Database is down");
        assertThat(response.getDescription()).isEqualTo("Cannot connect to the primary database");
        assertThat(response.getRequesterEmail()).isEqualTo("analyst@company.com");
        assertThat(response.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 15, 9, 0));
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 15, 10, 30));
    }

    @Test
    void updateRequestUpdatesOnlyUpdatableFields() {
        Ticket entity = ticket();

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("Database is back");
        request.setDescription("Connection restored");
        request.setPriority(Priority.MEDIUM);

        mapper.updateEntity(request, entity);

        assertThat(entity.getTitle()).isEqualTo("Database is back");
        assertThat(entity.getDescription()).isEqualTo("Connection restored");
        assertThat(entity.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getRequesterEmail()).isEqualTo("analyst@company.com");
        assertThat(entity.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(entity.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 15, 9, 0));
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 15, 10, 30));
    }

    @Test
    void mapsStatusChangeRequestToStatus() {
        StatusChangeRequest request = new StatusChangeRequest(Status.RESOLVED);

        assertThat(mapper.toStatus(request)).isEqualTo(Status.RESOLVED);
    }

    @Test
    void buildsPagedResponseFromSpringPage() {
        TicketResponse one = mapper.toResponse(ticket());
        TicketResponse two = mapper.toResponse(ticket());
        Page<TicketResponse> page = new PageImpl<>(List.of(one, two), PageRequest.of(2, 5), 12L);

        PagedTicketResponse response = PagedTicketResponse.from(page);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(12L);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }
}