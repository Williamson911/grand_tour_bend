package be.technifutur.grandtourbend.models.auth.responses;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username
) {
}