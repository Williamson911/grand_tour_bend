package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.eventType.responses.EventTypeResponse;

import java.util.List;

public interface EventTypeService {

    List<EventTypeResponse> getAll();
}