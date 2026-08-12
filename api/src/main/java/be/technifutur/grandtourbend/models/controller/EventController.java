package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.EventService;
import be.technifutur.grandtourbend.models.event.requests.EventRequest;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventIndexResponse>> findAll(
            @RequestParam Map<String, String> params,
            @RequestParam( required = false, defaultValue = "0") int page,
            @RequestParam( required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "date") String sort
    ) {
        var eventPage = eventService.findAll(params, PageRequest.of(page, size, Sort.by(sort)))
                .map(EventIndexResponse::fromEvent);
        return ResponseEntity.ok(eventPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventIndexResponse> finById(
            @PathVariable String id
    ) {

        var event = eventService.findById(id);

        var response = EventIndexResponse.fromEvent(event);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventIndexResponse> create(
            @Valid @RequestBody EventRequest request
    ) {
        String id = eventService.save(request.toEvent());

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(uri).build();
    }
}
