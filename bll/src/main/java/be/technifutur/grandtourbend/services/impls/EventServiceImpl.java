package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.EventService;
import be.technifutur.grandtourbend.SearchSpecification;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.EventType;
import be.technifutur.grandtourbend.exceptions.EventNameAlreadyExist;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.exceptions.EventTypeNotFoundException;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;

    @Override
    public Page<Event> findAll(Map<String, String> params, Pageable pageable) {

        List<Specification<Event>> specs = SearchSpecification.search(params);

        return eventRepository.findAll(
                Specification.allOf(specs),
                pageable
        );
    }

    @Override
    public Event findById(UUID id) {
        return eventRepository.findById(id).orElseThrow(() ->
                new EventNotFoundException("Event with id " + id + " not found")
        );
    }

    @Override
    public UUID save(Event event, Long eventTypeId) {
        if(eventRepository.existsByName(event.getName())){
            throw new EventNameAlreadyExist("Event with name " + event.getName() + " already exist");
        }

        EventType type = eventTypeRepository.findById(eventTypeId).orElseThrow(() ->
                new EventTypeNotFoundException("EventType with id " + eventTypeId + " not found")
        );
        event.setType(type);

        return eventRepository.save(event).getId();
    }
}
