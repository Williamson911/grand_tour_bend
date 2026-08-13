package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.EventService;
import be.technifutur.grandtourbend.RegistrationService;
import be.technifutur.grandtourbend.models.event.requests.EventRequest;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final RegistrationService registrationService;

    @GetMapping
    public ResponseEntity<Page<EventIndexResponse>> findAll(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @RequestParam Map<String, String> params,
            @RequestParam( required = false, defaultValue = "0") int page,
            @RequestParam( required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "date") String sort
    ) {
        Set<UUID> registeredIds = registrationService.registeredEventIds(session.id());
        var eventPage = eventService.findAll(params, PageRequest.of(page, size, Sort.by(sort)))
                .map(e -> EventIndexResponse.fromEvent(e, registeredIds.contains(e.getId())));
        return ResponseEntity.ok(eventPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventIndexResponse> finById(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {

        var event = eventService.findById(id);

        boolean registered = registrationService.isRegistered(session.id(), id);
        var response = EventIndexResponse.fromEvent(event, registered);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventIndexResponse> create(
            @Valid @RequestBody EventRequest request
    ) {
        UUID id = eventService.save(request.toEvent(), request.eventTypeId());

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(uri).build();
    }
}
