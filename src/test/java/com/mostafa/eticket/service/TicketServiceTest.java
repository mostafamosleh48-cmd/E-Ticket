package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Priority;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.PagedTicketResponse;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import com.mostafa.eticket.exception.InvalidPageSizeException;
import com.mostafa.eticket.exception.InvalidStatusTransitionException;
import com.mostafa.eticket.exception.TicketNotFoundException;
import com.mostafa.eticket.mapper.TicketMapper;
import com.mostafa.eticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private final TicketMapper mapper = Mappers.getMapper(TicketMapper.class);
    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, mapper);
    }

    private Ticket ticket(Long id, Status status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Database is down");
        ticket.setDescription("Cannot connect to the primary database");
        ticket.setRequesterEmail("analyst@company.com");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(status);
        ticket.setDueDate(LocalDate.now().plusDays(2));
        return ticket;
    }

    private CreateTicketRequest createRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Database is down");
        request.setDescription("Cannot connect to the primary database");
        request.setRequesterEmail("analyst@company.com");
        request.setPriority(Priority.HIGH);
        return request;
    }

    @Test
    void createTicketStartsOpenAndSaves() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        TicketResponse response = service.createTicket(createRequest());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Status.OPEN);
        assertThat(response.getTitle()).isEqualTo("Database is down");
        assertThat(response.getRequesterEmail()).isEqualTo("analyst@company.com");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void getTicketReturnsMappedResponse() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, Status.IN_PROGRESS)));

        TicketResponse response = service.getTicket(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Database is down");
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void getTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listTicketsReturnsAllWhenNoStatus() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN)), Pageable.ofSize(20), 1);
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(ticketRepository).findAll(any(Pageable.class));
        verify(ticketRepository, never()).findByStatus(any(), any());
    }

    @Test
    void listTicketsFiltersByStatus() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN)), Pageable.ofSize(20), 1);
        when(ticketRepository.findByStatus(any(Status.class), any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, Status.OPEN);

        assertThat(response.getContent()).hasSize(1);
        verify(ticketRepository).findByStatus(any(Status.class), any(Pageable.class));
        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listTicketsRejectsNegativePage() {
        assertThatThrownBy(() -> service.listTickets(-1, 20, null))
                .isInstanceOf(InvalidPageSizeException.class);
    }

    @Test
    void listTicketsRejectsOversizeSize() {
        assertThatThrownBy(() -> service.listTickets(0, 101, null))
                .isInstanceOf(InvalidPageSizeException.class);
    }

    @Test
    void updateTicketChangesOnlyUpdatableFields() {
        Ticket existing = ticket(1L, Status.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("Database is back");
        request.setDescription("Connection restored");
        request.setPriority(Priority.LOW);

        TicketResponse response = service.updateTicket(1L, request);

        assertThat(response.getTitle()).isEqualTo("Database is back");
        assertThat(response.getDescription()).isEqualTo("Connection restored");
        assertThat(response.getPriority()).isEqualTo(Priority.LOW);
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.getRequesterEmail()).isEqualTo("analyst@company.com");
        verify(ticketRepository).save(existing);
    }

    @Test
    void updateTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicket(99L, new UpdateTicketRequest()))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void changeStatusAcceptsLegalTransition() {
        Ticket existing = ticket(1L, Status.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));

        TicketResponse response = service.changeStatus(1L, new StatusChangeRequest(Status.IN_PROGRESS));

        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(ticketRepository).save(existing);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, Status.OPEN)));

        assertThatThrownBy(() -> service.changeStatus(1L, new StatusChangeRequest(Status.CLOSED)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("CLOSED");
    }

    @Test
    void changeStatusRejectsSameStatus() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, Status.IN_PROGRESS)));

        assertThatThrownBy(() -> service.changeStatus(1L, new StatusChangeRequest(Status.IN_PROGRESS)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void deleteTicketDeletesWhenExists() {
        when(ticketRepository.existsById(1L)).thenReturn(true);

        service.deleteTicket(1L);

        verify(ticketRepository).deleteById(1L);
    }

    @Test
    void deleteTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteTicket(99L))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketRepository, never()).deleteById(any());
    }
}