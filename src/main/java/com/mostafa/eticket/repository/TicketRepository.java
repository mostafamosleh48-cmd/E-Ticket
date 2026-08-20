package com.mostafa.eticket.repository;

import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByStatus(Status status, Pageable pageable);

    List<Ticket> findByDueDateBeforeAndStatusNotIn(LocalDate dueDate, Collection<Status> statuses);

    Page<Ticket> findByOrganization_Id(Long organizationId, Pageable pageable);

    Page<Ticket> findByOrganization_IdAndStatus(Long organizationId, Status status, Pageable pageable);

    Optional<Ticket> findByIdAndOrganization_Id(Long id, Long organizationId);
}