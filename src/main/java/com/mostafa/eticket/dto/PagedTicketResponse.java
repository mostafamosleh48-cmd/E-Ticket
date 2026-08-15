package com.mostafa.eticket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedTicketResponse {

    private List<TicketResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static PagedTicketResponse from(Page<TicketResponse> page) {
        return new PagedTicketResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}