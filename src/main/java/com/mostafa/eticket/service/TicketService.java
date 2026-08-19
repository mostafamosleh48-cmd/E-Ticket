package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.PagedTicketResponse;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.exception.InvalidOrganizationException;
import com.mostafa.eticket.exception.InvalidPageSizeException;
import com.mostafa.eticket.exception.InvalidStatusTransitionException;
import com.mostafa.eticket.exception.TicketNotFoundException;
import com.mostafa.eticket.mapper.TicketMapper;
import com.mostafa.eticket.repository.OrganizationRepository;
import com.mostafa.eticket.repository.TicketRepository;
import com.mostafa.eticket.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final OrganizationRepository organizationRepository;
    private final TicketMapper ticketMapper;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, AuthUser caller) {
        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setOrganization(resolveOrganization(caller, request));
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long id, AuthUser caller) {
        Ticket ticket = getScopedTicket(id, caller);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public PagedTicketResponse listTickets(int page, int size, Status status, AuthUser caller) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageSizeException("Page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE + " but got page=" + page + ", size=" + size);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Ticket> ticketPage;
        if (caller.role() == Role.ADMIN) {
            ticketPage = (status == null) ? ticketRepository.findAll(pageable) : ticketRepository.findByStatus(status, pageable);
        } else {
            ticketPage = (status == null)
                    ? ticketRepository.findByOrganization_Id(caller.organizationId(), pageable)
                    : ticketRepository.findByOrganization_IdAndStatus(caller.organizationId(), status, pageable);
        }
        return PagedTicketResponse.from(ticketPage.map(ticketMapper::toResponse));
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request, AuthUser caller) {
        Ticket ticket = getScopedTicket(id, caller);
        ticketMapper.updateEntity(request, ticket);
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);

    }

    @Transactional
    public TicketResponse changeStatus(Long id, StatusChangeRequest request, AuthUser caller) {
        Ticket ticket = getScopedTicket(id, caller);
        Status status = ticketMapper.toStatus(request);
        if (!ticket.getStatus().canTransitionTo(status)) {
            throw new InvalidStatusTransitionException("Ticket " + id + " cannot transition from " + ticket.getStatus() + " to " + status);
        }
        ticket.setStatus(status);
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public void deleteTicket(Long id, AuthUser caller) {
        Ticket ticket = getScopedTicket(id, caller);
        ticketRepository.delete(ticket);
    }

    private Organization resolveOrganization(AuthUser caller, CreateTicketRequest request) {
        if (caller.role() == Role.ADMIN) {
            Long organizationId = request.getOrganizationId();
            if (organizationId == null) {
                throw new InvalidOrganizationException("organizationId is required when creating a ticket as ADMIN");
            }
            return organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new InvalidOrganizationException("Organization not found with id " + organizationId));
        }
        if (request.getOrganizationId() != null) {
            throw new InvalidOrganizationException("Only ADMIN can specify organizationId");
        }
        return organizationRepository.findById(caller.organizationId())
                .orElseThrow(() -> new InvalidOrganizationException("Organization not found with id " + caller.organizationId()));
    }

    private Ticket getScopedTicket(Long id, AuthUser caller) {
        if (caller.role() == Role.ADMIN) {
            return ticketRepository.findById(id)
                    .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + id));
        }
        return ticketRepository.findByIdAndOrganization_Id(id, caller.organizationId())
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + id));
    }

}