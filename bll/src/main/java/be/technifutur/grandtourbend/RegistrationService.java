package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RegistrationService {
    List<EventIndexResponse> getAll(UUID userId);

    UUID register(UUID userId, UUID eventId);

    void unregister(UUID userId, UUID eventId);

    boolean isRegistered(UUID userId, UUID eventId);

    Set<UUID> registeredEventIds(UUID userId);
}
