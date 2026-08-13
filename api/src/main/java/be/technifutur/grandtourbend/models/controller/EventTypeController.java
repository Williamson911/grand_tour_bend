package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.EventTypeService;
import be.technifutur.grandtourbend.models.eventType.responses.EventTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Event Types", description = "Catégories d'events (nom, icône, couleur) utilisées par Event.type")
public class EventTypeController {

    private final EventTypeService eventTypeService;


    @GetMapping
    @Operation(summary = "Lister les types d'events")
    @ApiResponse(responseCode = "200", description = "Liste des types")
    public List<EventTypeResponse> getAll() {
        return eventTypeService.getAll();
    }
}
