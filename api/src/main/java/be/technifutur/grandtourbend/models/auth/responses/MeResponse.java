package be.technifutur.grandtourbend.models.auth.responses;

import be.technifutur.grandtourbend.entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String username,
        String email,
        String bandaiTcgId,
        LocalDateTime createdAt
) {

    public static MeResponse fromUser(User user) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getBandaiTcgId(),
                user.getCreatedAt()
        );
    }
}
