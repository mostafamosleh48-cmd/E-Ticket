package com.mostafa.eticket.mapper;

import com.mostafa.eticket.domain.Status;
import com.mostafa.eticket.domain.Ticket;
import com.mostafa.eticket.dto.CreateTicketRequest;
import com.mostafa.eticket.dto.StatusChangeRequest;
import com.mostafa.eticket.dto.TicketResponse;
import com.mostafa.eticket.dto.UpdateTicketRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    Ticket toEntity(CreateTicketRequest request);

    @Mapping(target = "organizationId", source = "organization.id")
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requesterEmail", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void updateEntity(UpdateTicketRequest request, @MappingTarget Ticket ticket);

    default Status toStatus(StatusChangeRequest request) {
        return request.getStatus();
    }
}