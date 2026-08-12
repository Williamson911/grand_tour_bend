package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.eventType.Responses.EventTypeResponse;

import java.util.List;

public interface EventTypeService {

    List<EventTypeResponse> getAll();
}