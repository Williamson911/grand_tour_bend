package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.RegistrationService;
import be.technifutur.grandtourbend.models.event.reponses.EventIndexResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
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
public class RegistrationController {
    private final RegistrationService registrationService;

    @GetMapping("/me")
    public ResponseEntity<List<EventIndexResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(registrationService.getAll(session.id()));
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        registrationService.register(session.id(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        registrationService.unregister(session.id(), eventId);
        return ResponseEntity.noContent().build();
    }
}
