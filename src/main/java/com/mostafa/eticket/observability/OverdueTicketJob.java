package com.mostafa.eticket.observability;

import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OverdueTicketJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueTicketJob.class);

    private final TicketRepository ticketRepository;

    @Scheduled(fixedDelayString = "${eticket.overdue.fixed-delay:60000}")
    @Transactional(readOnly = true)
    public void detectOverdueTickets() {
        List<Ticket> overdue = ticketRepository.findByDueDateBeforeAndStatusNotIn(
                LocalDate.now(), List.of(Status.RESOLVED, Status.CLOSED));
        if (overdue.isEmpty()) {
            log.info("Overdue ticket scan: no overdue tickets found");
            return;
        }
        for (Ticket ticket : overdue) {
            log.warn("Overdue ticket: id={}, title={}, dueDate={}, status={}, organizationId={}",
                    ticket.getId(), ticket.getTitle(), ticket.getDueDate(), ticket.getStatus(),
                    ticket.getOrganization().getId());
        }
        log.warn("Overdue ticket scan complete: {} overdue ticket(s) found", overdue.size());
    }
}