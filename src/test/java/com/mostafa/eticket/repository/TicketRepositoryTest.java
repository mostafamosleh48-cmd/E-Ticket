package com.mostafa.eticket.repository;

import com.mostafa.eticket.configuration.JpaAuditingConfig;
import com.mostafa.eticket.domain.Priority;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    private Ticket ticket(Status status) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Database is down");
        ticket.setDescription("Cannot connect to the primary database");
        ticket.setRequesterEmail("analyst@company.com");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(status);
        ticket.setDueDate(LocalDate.now().plusDays(2));
        return ticket;
    }

    @Test
    void savesTicketWithAuditTimestamps() {
        Ticket saved = ticketRepository.save(ticket(Status.OPEN));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    void findsPageOfTicketsOrderedNewestFirst() {
        Ticket first = ticketRepository.save(ticket(Status.OPEN));
        Ticket second = ticketRepository.save(ticket(Status.IN_PROGRESS));

        Page<Ticket> page = ticketRepository.findAll(
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(page.getContent().get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void filtersTicketsByStatus() {
        ticketRepository.save(ticket(Status.OPEN));
        ticketRepository.save(ticket(Status.IN_PROGRESS));

        Page<Ticket> open = ticketRepository.findByStatus(Status.OPEN, PageRequest.of(0, 20));
        Page<Ticket> inProgress = ticketRepository.findByStatus(Status.IN_PROGRESS, PageRequest.of(0, 20));

        assertThat(open.getContent()).hasSize(1);
        assertThat(inProgress.getContent()).hasSize(1);
    }

    @Test
    void statusFilterReturnsEmptyPageWhenNoMatches() {
        ticketRepository.save(ticket(Status.RESOLVED));

        Page<Ticket> closed = ticketRepository.findByStatus(Status.CLOSED, PageRequest.of(0, 20));

        assertThat(closed.getContent()).isEmpty();
        assertThat(closed.getTotalElements()).isZero();
    }
}