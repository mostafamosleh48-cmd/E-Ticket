package com.mostafa.eticket.controller;

import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.*;
import com.mostafa.eticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tickets", description = "APIs for managing tickets")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
  private final TicketService ticketService;

  @Operation(
      summary = "List tickets",
      description = "Returns a paginated list of tickets with optional status filtering.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tickets retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
  })
  @GetMapping
  public ResponseEntity<PagedTicketResponse> getAllTickets(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) Status status) {

    return ResponseEntity.ok(ticketService.listTickets(page, size, status));
  }

  @Operation(summary = "Get ticket by ID", description = "Returns a single ticket by its ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ticket found"),
    @ApiResponse(responseCode = "404", description = "Ticket not found")
  })
  @GetMapping("/{id}")
  public ResponseEntity<TicketResponse> getTicket(@PathVariable long id) {
    return ResponseEntity.ok(ticketService.getTicket(id));
  }

  @Operation(summary = "Create ticket", description = "Creates a new ticket.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Ticket created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid ticket data")
  })
  @PostMapping
  public ResponseEntity<TicketResponse> createTicket(
      @Valid @RequestBody CreateTicketRequest createTicketRequest) {
    TicketResponse ticketResponse = ticketService.createTicket(createTicketRequest);
    return ResponseEntity.created(URI.create("/api/v1/tickets/" + ticketResponse.getId()))
        .body(ticketResponse);
  }

  @Operation(summary = "Update ticket", description = "Updates an existing ticket.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ticket updated successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid ticket data"),
    @ApiResponse(responseCode = "404", description = "Ticket not found")
  })
  @PutMapping("/{id}")
  public ResponseEntity<TicketResponse> updateTicket(
      @PathVariable long id, @Valid @RequestBody UpdateTicketRequest updateTicketRequest) {
    TicketResponse ticketResponse = ticketService.updateTicket(id, updateTicketRequest);
    return ResponseEntity.ok(ticketResponse);
  }

  @Operation(
      summary = "Change ticket status",
      description = "Changes the status of an existing ticket.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ticket status changed successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid status transition or invalid request"),
    @ApiResponse(responseCode = "404", description = "Ticket not found")
  })
  @PatchMapping("/{id}/status")
  public ResponseEntity<TicketResponse> changeTicketStatus(
      @PathVariable long id, @Valid @RequestBody StatusChangeRequest statusChangeRequest) {
    TicketResponse response = ticketService.changeStatus(id, statusChangeRequest);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Delete ticket", description = "Deletes an existing ticket.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Ticket deleted successfully"),
    @ApiResponse(responseCode = "404", description = "Ticket not found")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTicket(@PathVariable long id) {
    ticketService.deleteTicket(id);
    return ResponseEntity.noContent().build();
  }
}
