package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.ResultsService;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.models.results.responses.ResultsResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/results")
@CrossOrigin("*")
@Tag(name = "Results", description = "Résultat de tournoi (placement, deck, matchs) de l'utilisateur connecté pour un event")
public class ResultsController {
    private final ResultsService resultsService;

    @GetMapping("/me")
    @Operation(summary = "Lister mes résultats", description = "Renvoie tous les résultats de l'utilisateur connecté, tous events confondus.")
    @ApiResponse(responseCode = "200", description = "Liste des résultats")
    public ResponseEntity<List<ResultsResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(resultsService.getAll(session.id()));
    }

    @PutMapping("/{eventId}")
    @Operation(
            summary = "Enregistrer mon résultat pour un event",
            description = "Upsert : crée le résultat s'il n'existe pas encore pour cet event, sinon met à jour l'existant (un seul résultat par utilisateur et par event)."
    )
    @ApiResponse(responseCode = "200", description = "Résultat créé ou mis à jour")
    @ApiResponse(responseCode = "404", description = "Event introuvable")
    public ResponseEntity<Void> save(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId,
            @Valid @RequestBody ResultsRequest request
    ) {
        resultsService.create(session.id(), eventId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Effacer mon résultat pour un event")
    @ApiResponse(responseCode = "204", description = "Résultat supprimé")
    @ApiResponse(responseCode = "404", description = "Aucun résultat pour cet event")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        resultsService.delete(session.id(), eventId);
        return ResponseEntity.noContent().build();
    }
}
