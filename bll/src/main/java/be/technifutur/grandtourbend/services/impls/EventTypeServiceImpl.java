package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.EventTypeService;
import be.technifutur.grandtourbend.models.eventType.responses.EventTypeResponse;
import be.technifutur.grandtourbend.repositories.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventTypeServiceImpl implements EventTypeService {

    private final EventTypeRepository eventTypeRepository;

    @Override
    public List<EventTypeResponse> getAll() {
        return eventTypeRepository.findAll()
                .stream()
                .map(EventTypeResponse::fromEventType)
                .toList();
    }
}