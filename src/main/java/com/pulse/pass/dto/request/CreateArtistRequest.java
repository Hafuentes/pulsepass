package com.pulse.pass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArtistRequest(
        @NotBlank(message = "Stage name is required") @Size(max = 100) String stageName,
        @Size(max = 100) String country,
        @Size(max = 50) String genre
) {}
