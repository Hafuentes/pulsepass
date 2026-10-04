package com.pulse.pass.mapper;

import org.mapstruct.Mapper;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.dto.response.ArtistResponse;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
