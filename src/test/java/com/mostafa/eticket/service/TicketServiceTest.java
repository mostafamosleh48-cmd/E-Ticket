package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Priority;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.PagedTicketResponse;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import com.mostafa.eticket.exception.InvalidOrganizationException;
import com.mostafa.eticket.exception.InvalidPageSizeException;
import com.mostafa.eticket.exception.InvalidStatusTransitionException;
import com.mostafa.eticket.exception.TicketNotFoundException;
import com.mostafa.eticket.mapper.TicketMapper;
import com.mostafa.eticket.repository.OrganizationRepository;
import com.mostafa.eticket.repository.TicketRepository;
import com.mostafa.eticket.security.AuthUser;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private final TicketMapper mapper = Mappers.getMapper(TicketMapper.class);
    private TicketService service;

    private static final Long ORG_ID = 10L;

    private static final AuthUser AGENT = new AuthUser("alice", Role.AGENT, ORG_ID);
    private static final AuthUser ADMIN = new AuthUser("root", Role.ADMIN, null);
    private static final AuthUser VIEWER = new AuthUser("viewer", Role.VIEWER, ORG_ID);

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, organizationRepository, mapper);
    }

    private Ticket ticket(Long id, Status status, Long organizationId) {
        Organization organization = new Organization();
        organization.setId(organizationId);
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Database is down");
        ticket.setDescription("Cannot connect to the primary database");
        ticket.setRequesterEmail("analyst@company.com");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(status);
        ticket.setDueDate(LocalDate.now().plusDays(2));
        ticket.setOrganization(organization);
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

    private Organization organization() {
        Organization organization = new Organization();
        organization.setId(ORG_ID);
        organization.setName("Acme");
        return organization;
    }

    @Test
    void createTicketBindsCallersOrganizationAndSaves() {
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        TicketResponse response = service.createTicket(createRequest(), AGENT);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Status.OPEN);
        assertThat(response.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(response.getTitle()).isEqualTo("Database is down");
        assertThat(response.getRequesterEmail()).isEqualTo("analyst@company.com");
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicketAsAdminRequiresOrganizationId() {
        CreateTicketRequest request = createRequest();

        assertThatThrownBy(() -> service.createTicket(request, ADMIN))
                .isInstanceOf(InvalidOrganizationException.class)
                .hasMessageContaining("organizationId is required");
    }

    @Test
    void createTicketAsAdminBindsSpecifiedOrganization() {
        Organization other = new Organization();
        other.setId(42L);
        when(organizationRepository.findById(42L)).thenReturn(Optional.of(other));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(2L);
            return ticket;
        });

        CreateTicketRequest request = createRequest();
        request.setOrganizationId(42L);

        TicketResponse response = service.createTicket(request, ADMIN);

        assertThat(response.getOrganizationId()).isEqualTo(42L);
    }

    @Test
    void createTicketAsAdminWithUnknownOrganizationThrows() {
        when(organizationRepository.findById(42L)).thenReturn(Optional.empty());

        CreateTicketRequest request = createRequest();
        request.setOrganizationId(42L);

        assertThatThrownBy(() -> service.createTicket(request, ADMIN))
                .isInstanceOf(InvalidOrganizationException.class)
                .hasMessageContaining("Organization not found");
    }

    @Test
    void createTicketRejectsOrganizationIdFromNonAdmin() {
        CreateTicketRequest request = createRequest();
        request.setOrganizationId(42L);

        assertThatThrownBy(() -> service.createTicket(request, AGENT))
                .isInstanceOf(InvalidOrganizationException.class)
                .hasMessageContaining("Only ADMIN");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketReturnsScopedMappedResponse() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID))
                .thenReturn(Optional.of(ticket(1L, Status.IN_PROGRESS, ORG_ID)));

        TicketResponse response = service.getTicket(1L, AGENT);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.getOrganizationId()).isEqualTo(ORG_ID);
    }

    @Test
    void getTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(99L, AGENT))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getTicketFromAnotherOrganizationReturns404() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(1L, AGENT))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketRepository, never()).findById(1L);
    }

    @Test
    void adminGetTicketSeesAnyOrganization() {
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket(7L, Status.OPEN, 99L)));

        TicketResponse response = service.getTicket(7L, ADMIN);

        assertThat(response.getOrganizationId()).isEqualTo(99L);
    }

    @Test
    void listTicketsScopedToCallersOrganizationWhenNoStatus() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN, ORG_ID)), Pageable.ofSize(20), 1);
        when(ticketRepository.findByOrganization_Id(eq(ORG_ID), any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, null, AGENT);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(ticketRepository).findByOrganization_Id(eq(ORG_ID), any(Pageable.class));
        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listTicketsScopesByStatusForCallersOrganization() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN, ORG_ID)), Pageable.ofSize(20), 1);
        when(ticketRepository.findByOrganization_IdAndStatus(eq(ORG_ID), eq(Status.OPEN), any(Pageable.class)))
                .thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, Status.OPEN, AGENT);

        assertThat(response.getContent()).hasSize(1);
        verify(ticketRepository).findByOrganization_IdAndStatus(eq(ORG_ID), eq(Status.OPEN), any(Pageable.class));
        verify(ticketRepository, never()).findByStatus(any(), any());
    }

    @Test
    void adminListTicketsSeesAll() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN, 99L)), Pageable.ofSize(20), 1);
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, null, ADMIN);

        assertThat(response.getContent()).hasSize(1);
        verify(ticketRepository).findAll(any(Pageable.class));
        verify(ticketRepository, never()).findByOrganization_Id(any(), any());
    }

    @Test
    void adminListTicketsFiltersByStatusAcrossAll() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN, 99L)), Pageable.ofSize(20), 1);
        when(ticketRepository.findByStatus(eq(Status.OPEN), any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, Status.OPEN, ADMIN);

        assertThat(response.getContent()).hasSize(1);
        verify(ticketRepository).findByStatus(eq(Status.OPEN), any(Pageable.class));
    }

    @Test
    void viewerCanListTicketsInTheirOrganization() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket(1L, Status.OPEN, ORG_ID)), Pageable.ofSize(20), 1);
        when(ticketRepository.findByOrganization_Id(eq(ORG_ID), any(Pageable.class))).thenReturn(page);

        PagedTicketResponse response = service.listTickets(0, 20, null, VIEWER);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void listTicketsRejectsNegativePage() {
        assertThatThrownBy(() -> service.listTickets(-1, 20, null, AGENT))
                .isInstanceOf(InvalidPageSizeException.class);
    }

    @Test
    void listTicketsRejectsOversizeSize() {
        assertThatThrownBy(() -> service.listTickets(0, 101, null, AGENT))
                .isInstanceOf(InvalidPageSizeException.class);
    }

    @Test
    void updateTicketChangesOnlyUpdatableFields() {
        Ticket existing = ticket(1L, Status.IN_PROGRESS, ORG_ID);
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(existing));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("Database is back");
        request.setDescription("Connection restored");
        request.setPriority(Priority.LOW);

        TicketResponse response = service.updateTicket(1L, request, AGENT);

        assertThat(response.getTitle()).isEqualTo("Database is back");
        assertThat(response.getDescription()).isEqualTo("Connection restored");
        assertThat(response.getPriority()).isEqualTo(Priority.LOW);
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.getRequesterEmail()).isEqualTo("analyst@company.com");
        verify(ticketRepository).save(existing);
    }

    @Test
    void updateTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicket(99L, new UpdateTicketRequest(), AGENT))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void updateTicketFromAnotherOrganizationReturns404() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicket(1L, new UpdateTicketRequest(), AGENT))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void changeStatusAcceptsLegalTransition() {
        Ticket existing = ticket(1L, Status.OPEN, ORG_ID);
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(existing));

        TicketResponse response = service.changeStatus(1L, new StatusChangeRequest(Status.IN_PROGRESS), AGENT);

        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(ticketRepository).save(existing);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID))
                .thenReturn(Optional.of(ticket(1L, Status.OPEN, ORG_ID)));

        assertThatThrownBy(() -> service.changeStatus(1L, new StatusChangeRequest(Status.CLOSED), AGENT))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("CLOSED");
    }

    @Test
    void changeStatusRejectsSameStatus() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID))
                .thenReturn(Optional.of(ticket(1L, Status.IN_PROGRESS, ORG_ID)));

        assertThatThrownBy(() -> service.changeStatus(1L, new StatusChangeRequest(Status.IN_PROGRESS), AGENT))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void deleteTicketDeletesScopedTicketWhenExists() {
        when(ticketRepository.findByIdAndOrganization_Id(1L, ORG_ID))
                .thenReturn(Optional.of(ticket(1L, Status.OPEN, ORG_ID)));

        service.deleteTicket(1L, AGENT);

        verify(ticketRepository).delete(any(Ticket.class));
    }

    @Test
    void deleteTicketThrowsNotFoundWhenMissing() {
        when(ticketRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTicket(99L, AGENT))
                .isInstanceOf(TicketNotFoundException.class);
        verify(ticketRepository, never()).delete(any());
    }
}