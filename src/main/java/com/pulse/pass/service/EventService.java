package com.pulse.pass.service;

import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;

public interface EventService {

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse create(CreateEventRequest request);

    @PostAuthorize("@authz.canViewEvent(returnObject)")
    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse publish(String eventCode);

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse addArtist(String eventCode, Long artistId);

    @PostFilter("@authz.canViewEventSummary(filterObject)")
    List<EventSummaryResponse> findByArtist(String stageName);
}
