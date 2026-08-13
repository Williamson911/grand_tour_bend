package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.entities.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface EventService {
    Page<Event> findAll(Map<String, String> params, Pageable pageable);

    Event findById(UUID id);

    UUID save(Event event, Long eventTypeId);
}
