package com.mostafa.eticket.observability;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class OverdueTicketJobTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private OverdueTicketJob overdueTicketJob;

    @Test
    void reportsEachOverdueTicketAndASummaryCount(CapturedOutput output) {
        when(ticketRepository.findByDueDateBeforeAndStatusNotIn(any(LocalDate.class), any()))
                .thenReturn(List.of(
                        ticket(1L, "Login bug", Status.OPEN, 10L),
                        ticket(2L, "Pricing page", Status.IN_PROGRESS, 20L)));

        overdueTicketJob.detectOverdueTickets();

        verify(ticketRepository).findByDueDateBeforeAndStatusNotIn(
                any(LocalDate.class), eq(List.of(Status.RESOLVED, Status.CLOSED)));
        assertThat(output)
                .contains("Overdue ticket: id=1")
                .contains("Overdue ticket: id=2")
                .contains("2 overdue ticket(s) found");
    }

    @Test
    void logsWhenNothingIsOverdue(CapturedOutput output) {
        when(ticketRepository.findByDueDateBeforeAndStatusNotIn(any(LocalDate.class), any()))
                .thenReturn(List.of());

        overdueTicketJob.detectOverdueTickets();

        assertThat(output).contains("no overdue tickets found");
    }

    private Ticket ticket(Long id, String title, Status status, Long organizationId) {
        Organization organization = new Organization();
        organization.setId(organizationId);
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle(title);
        ticket.setStatus(status);
        ticket.setDueDate(LocalDate.now().minusDays(1));
        ticket.setOrganization(organization);
        return ticket;
    }
}