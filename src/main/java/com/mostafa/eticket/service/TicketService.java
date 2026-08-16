package com.mostafa.eticket.service;

import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.PagedTicketResponse;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.exception.InvalidPageSizeException;
import com.mostafa.eticket.exception.InvalidStatusTransitionException;
import com.mostafa.eticket.exception.TicketNotFoundException;
import com.mostafa.eticket.mapper.TicketMapper;
import com.mostafa.eticket.repository.TicketRepository;
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
    private final TicketMapper ticketMapper;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = ticketMapper.toEntity(request);
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + id));
        return ticketMapper.toResponse(ticket);
    }

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public PagedTicketResponse listTickets(int page, int size, Status status) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageSizeException("Page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE + " but got page=" + page + ", size=" + size);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Ticket> ticketPage = (status == null) ? ticketRepository.findAll(pageable) : ticketRepository.findByStatus(status, pageable);
        return PagedTicketResponse.from(ticketPage.map(ticketMapper::toResponse));
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + id));
        ticketMapper.updateEntity(request, ticket);
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);

    }

    @Transactional
    public TicketResponse changeStatus(Long id, StatusChangeRequest request) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + id));
        Status status = ticketMapper.toStatus(request);
        if (!ticket.getStatus().canTransitionTo(status)) {
            throw new InvalidStatusTransitionException("Ticket " + id + " cannot transition from " + ticket.getStatus() + " to " + status);
        }
        ticket.setStatus(status);
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found with id " + id);
        }
        ticketRepository.deleteById(id);
    }

}
