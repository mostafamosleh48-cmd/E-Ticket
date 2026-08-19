package com.mostafa.eticket.repository;

import com.mostafa.eticket.configuration.JpaAuditingConfig;
import com.mostafa.eticket.domain.Organization;
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

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization() {
        Organization organization = new Organization();
        organization.setName("org-" + System.nanoTime());
        return organizationRepository.save(organization);
    }

    private Ticket ticket(Status status, Organization organization) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Database is down");
        ticket.setDescription("Cannot connect to the primary database");
        ticket.setRequesterEmail("analyst@company.com");
        ticket.setPriority(Priority.HIGH);
        ticket.setStatus(status);
        ticket.setDueDate(LocalDate.now().plusDays(2));
        ticket.setOrganization(organization);
        return ticket;
    }

    @Test
    void savesTicketWithAuditTimestamps() {
        Organization organization = organization();
        Ticket saved = ticketRepository.save(ticket(Status.OPEN, organization));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Status.OPEN);
        assertThat(saved.getOrganization().getId()).isEqualTo(organization.getId());
    }

    @Test
    void findsPageOfTicketsOrderedNewestFirst() {
        Organization organization = organization();
        Ticket first = ticketRepository.save(ticket(Status.OPEN, organization));
        Ticket second = ticketRepository.save(ticket(Status.IN_PROGRESS, organization));

        Page<Ticket> page = ticketRepository.findAll(
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(page.getContent().get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void filtersTicketsByStatus() {
        Organization organization = organization();
        ticketRepository.save(ticket(Status.OPEN, organization));
        ticketRepository.save(ticket(Status.IN_PROGRESS, organization));

        Page<Ticket> open = ticketRepository.findByStatus(Status.OPEN, PageRequest.of(0, 20));
        Page<Ticket> inProgress = ticketRepository.findByStatus(Status.IN_PROGRESS, PageRequest.of(0, 20));

        assertThat(open.getContent()).hasSize(1);
        assertThat(inProgress.getContent()).hasSize(1);
    }

    @Test
    void statusFilterReturnsEmptyPageWhenNoMatches() {
        Organization organization = organization();
        ticketRepository.save(ticket(Status.RESOLVED, organization));

        Page<Ticket> closed = ticketRepository.findByStatus(Status.CLOSED, PageRequest.of(0, 20));

        assertThat(closed.getContent()).isEmpty();
        assertThat(closed.getTotalElements()).isZero();
    }

    @Test
    void scopesTicketsByOrganization() {
        Organization orgA = organization();
        Organization orgB = organization();
        ticketRepository.save(ticket(Status.OPEN, orgA));
        ticketRepository.save(ticket(Status.OPEN, orgB));

        Page<Ticket> pageA = ticketRepository.findByOrganization_Id(orgA.getId(), PageRequest.of(0, 20));
        Page<Ticket> pageB = ticketRepository.findByOrganization_Id(orgB.getId(), PageRequest.of(0, 20));

        assertThat(pageA.getContent()).hasSize(1);
        assertThat(pageA.getContent().get(0).getOrganization().getId()).isEqualTo(orgA.getId());
        assertThat(pageB.getContent()).hasSize(1);
        assertThat(pageB.getContent().get(0).getOrganization().getId()).isEqualTo(orgB.getId());
    }

    @Test
    void scopesTicketsByOrganizationAndStatus() {
        Organization organization = organization();
        ticketRepository.save(ticket(Status.OPEN, organization));
        ticketRepository.save(ticket(Status.CLOSED, organization));

        Page<Ticket> open = ticketRepository.findByOrganization_IdAndStatus(
                organization.getId(), Status.OPEN, PageRequest.of(0, 20));

        assertThat(open.getContent()).hasSize(1);
        assertThat(open.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    void findsTicketByIdAndOrganizationOnlyWhenSameOrganization() {
        Organization orgA = organization();
        Organization orgB = organization();
        Ticket owned = ticketRepository.save(ticket(Status.OPEN, orgA));

        assertThat(ticketRepository.findByIdAndOrganization_Id(owned.getId(), orgA.getId())).isPresent();
        assertThat(ticketRepository.findByIdAndOrganization_Id(owned.getId(), orgB.getId())).isEmpty();
    }
}