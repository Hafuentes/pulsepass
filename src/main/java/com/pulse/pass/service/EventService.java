package com.pulse.pass.service;

import java.util.List;

import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;

public interface EventService {

    EventResponse create(CreateEventRequest request);

    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    EventResponse publish(String eventCode);

    EventResponse addArtist(String eventCode, Long artistId);

    List<EventSummaryResponse> findByArtist(String stageName);
}
