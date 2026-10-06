package com.pulse.pass.dto.response;

public record ArtistResponse(
        Long id,
        String stageName,
        String country,
        String genre,
        boolean active
) {
}
