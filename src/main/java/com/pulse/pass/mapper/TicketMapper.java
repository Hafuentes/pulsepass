package com.pulse.pass.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.pulse.pass.domain.Ticket;
import com.pulse.pass.dto.response.TicketResponse;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "eventCode", source = "event.eventCode")
    @Mapping(target = "eventName", source = "event.name")
    TicketResponse toResponse(Ticket ticket);
}
