package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.RegistrationService;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Registration;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.AlreadyRegisteredException;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.exceptions.RegistrationNotFoundException;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.RegistrationRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventIndexResponse> getAll(UUID userId) {
        return registrationRepository.findByUser_Id(userId)
                .stream()
                .map(registration -> EventIndexResponse.fromEvent(registration.getEvent(), true))
                .toList();
    }

    @Override
    public UUID register(UUID userId, UUID eventId) {
        if (registrationRepository.findByUser_IdAndEvent_Id(userId, eventId).isPresent()) {
            throw new AlreadyRegisteredException("Already registered for this event");
        }

        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new EventNotFoundException("Event with id " + eventId + " not found")
        );

        Registration registration = new Registration(user, event);

        return registrationRepository.save(registration).getId();
    }

    @Override
    public void unregister(UUID userId, UUID eventId) {
        Registration registration = registrationRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .orElseThrow(() -> new RegistrationNotFoundException("Not registered for this event"));

        registrationRepository.delete(registration);
    }

    @Override
    public boolean isRegistered(UUID userId, UUID eventId) {
        return registrationRepository.findByUser_IdAndEvent_Id(userId, eventId).isPresent();
    }

    @Override
    public Set<UUID> registeredEventIds(UUID userId) {
        return registrationRepository.findByUser_Id(userId)
                .stream()
                .map(registration -> registration.getEvent().getId())
                .collect(Collectors.toSet());
    }
}
