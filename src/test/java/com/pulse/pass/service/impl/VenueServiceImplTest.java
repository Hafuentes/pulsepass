package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.VenueMapper;
import com.pulse.pass.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;
    @Mock
    private VenueMapper venueMapper;
    @InjectMocks
    private VenueServiceImpl service;

    private static final VenueResponse RESPONSE =
            new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);

    @Test
    void findByCode_existing_returnsDto() {
        // ARRANGE
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(RESPONSE);

        // ACT
        VenueResponse result = service.findByCode("VEN-SMR-01");

        // ASSERT
        assertThat(result).isEqualTo(RESPONSE);
    }

    @Test
    void findByCode_missing_throwsResourceNotFound() {
        // ARRANGE
        when(venueRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void findActiveVenues_returnsMappedActiveVenues() {
        // ARRANGE
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(RESPONSE);

        // ACT
        List<VenueResponse> result = service.findActiveVenues();

        // ASSERT
        assertThat(result).containsExactly(RESPONSE);
        verify(venueRepository).findByActiveTrueOrderByNameAsc();
    }
}
