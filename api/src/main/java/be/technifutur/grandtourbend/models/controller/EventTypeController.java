package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.EventTypeService;
import be.technifutur.grandtourbend.models.eventType.Responses.EventTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event-type")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EventTypeController {

    private final EventTypeService eventTypeService;


    @GetMapping
    public List<EventTypeResponse> getAll() {
        return eventTypeService.getAll();
    }
}
