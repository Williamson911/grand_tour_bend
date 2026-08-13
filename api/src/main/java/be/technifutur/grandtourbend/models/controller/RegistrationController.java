package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.RegistrationService;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/register")
@CrossOrigin("*")
@Tag(name = "Registration", description = "Inscription de l'utilisateur connecté aux events (\"je participe\")")
public class RegistrationController {
    private final RegistrationService registrationService;

    @GetMapping("/me")
    @Operation(summary = "Lister mes inscriptions", description = "Renvoie les events auxquels l'utilisateur connecté est inscrit.")
    @ApiResponse(responseCode = "200", description = "Liste des events")
    public ResponseEntity<List<EventIndexResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(registrationService.getAll(session.id()));
    }

    @PostMapping("/{eventId}")
    @Operation(summary = "S'inscrire à un event")
    @ApiResponse(responseCode = "201", description = "Inscription créée")
    @ApiResponse(responseCode = "404", description = "Event introuvable")
    @ApiResponse(responseCode = "409", description = "Déjà inscrit à cet event")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        registrationService.register(session.id(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Se désinscrire d'un event")
    @ApiResponse(responseCode = "204", description = "Désinscription effectuée")
    @ApiResponse(responseCode = "404", description = "Aucune inscription pour cet event")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        registrationService.unregister(session.id(), eventId);
        return ResponseEntity.noContent().build();
    }
}
