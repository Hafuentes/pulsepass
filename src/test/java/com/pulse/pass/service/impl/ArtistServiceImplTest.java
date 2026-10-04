package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.ArtistMapper;
import com.pulse.pass.repository.ArtistRepository;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtistMapper artistMapper;
    @InjectMocks
    private ArtistServiceImpl service;

    private final Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);
    private final ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);

    @Test
    void findById_existing_returnsDto() {
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(service.findById(1L)).isEqualTo(response);
    }

    @Test
    void findById_missing_throwsResourceNotFound() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStageName_isCaseInsensitive() {
        when(artistRepository.findByStageNameIgnoreCase("solar beat")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(service.findByStageName("solar beat")).isEqualTo(response);
    }

    @Test
    void findByStageName_missing_throwsResourceNotFound() {
        when(artistRepository.findByStageNameIgnoreCase("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByStageName("Ghost")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findActiveArtists_returnsOnlyWhatRepositoryReturnsMapped() {
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(service.findActiveArtists()).containsExactly(response);
    }
}
