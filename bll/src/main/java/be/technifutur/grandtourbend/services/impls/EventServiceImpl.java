package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.EventService;
import be.technifutur.grandtourbend.SearchSpecification;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.exceptions.EventNameAlreadyExist;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    @Override
    public Page<Event> findAll(Map<String, String> params, Pageable pageable) {

        List<Specification<Event>> specs = SearchSpecification.search(params);

        return eventRepository.findAll(
                Specification.allOf(specs),
                pageable
        );
    }

    @Override
    public Event findById(String id) {
        return eventRepository.findById(id).orElseThrow(() ->
                new EventNotFoundException("Event with id " + id + " not found")
        );
    }

    @Override
    public String save(Event event) {
        if(eventRepository.existsByName(event.getName())){
            throw new EventNameAlreadyExist("Event with name " + event.getName() + " already exist");
        }

        return eventRepository.save(event).getId();
    }
}
