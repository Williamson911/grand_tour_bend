package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.EventService;
import be.technifutur.grandtourbend.RegistrationService;
import be.technifutur.grandtourbend.models.event.requests.EventRequest;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Events", description = "Gestion des tournois/events du circuit")
public class EventController {
    private final EventService eventService;
    private final RegistrationService registrationService;

    @GetMapping
    @Operation(summary = "Lister les events", description = "Liste paginée des events, avec le statut d'inscription de l'utilisateur connecté pour chacun.")
    @ApiResponse(responseCode = "200", description = "Page d'events")
    public ResponseEntity<Page<EventIndexResponse>> findAll(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @RequestParam Map<String, String> params,
            @RequestParam( required = false, defaultValue = "0") int page,
            @RequestParam( required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "date") String sort
    ) {
        Set<UUID> registeredIds = session != null
                ? registrationService.registeredEventIds(session.id())
                : Set.of();
        var eventPage = eventService.findAll(params, PageRequest.of(page, size, Sort.by(sort)))
                .map(e -> EventIndexResponse.fromEvent(e, registeredIds.contains(e.getId())));
        return ResponseEntity.ok(eventPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un event")
    @ApiResponse(responseCode = "200", description = "Event trouvé")
    @ApiResponse(responseCode = "404", description = "Event introuvable")
    public ResponseEntity<EventIndexResponse> finById(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {

        var event = eventService.findById(id);

        boolean registered = session != null && registrationService.isRegistered(session.id(), id);
        var response = EventIndexResponse.fromEvent(event, registered);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Créer un event", description = "Réservé aux administrateurs.")
    @ApiResponse(responseCode = "201", description = "Event créé")
    @ApiResponse(responseCode = "403", description = "Réservé au rôle ADMIN")
    @ApiResponse(responseCode = "404", description = "EventType introuvable")
    @ApiResponse(responseCode = "409", description = "Un event porte déjà ce nom")
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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un event", description = "Réservé aux administrateurs.")
    @ApiResponse(responseCode = "204", description = "Event modifié")
    @ApiResponse(responseCode = "403", description = "Réservé au rôle ADMIN")
    @ApiResponse(responseCode = "404", description = "Event ou EventType introuvable")
    @ApiResponse(responseCode = "409", description = "Un event porte déjà ce nom")
    public ResponseEntity<Void> update(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request
    ) {
        eventService.update(id, request.toEvent(), request.eventTypeId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un event", description = "Réservé aux administrateurs.")
    @ApiResponse(responseCode = "204", description = "Event supprimé")
    @ApiResponse(responseCode = "403", description = "Réservé au rôle ADMIN")
    @ApiResponse(responseCode = "404", description = "Event introuvable")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
