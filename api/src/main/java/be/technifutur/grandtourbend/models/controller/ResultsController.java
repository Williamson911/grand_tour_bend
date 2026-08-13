package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.ResultsService;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.models.results.responses.ResultsResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
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
public class ResultsController {
    private final ResultsService resultsService;

    @GetMapping("/me")
    public ResponseEntity<List<ResultsResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(resultsService.getAll(session.id()));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<Void> save(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId,
            @Valid @RequestBody ResultsRequest request
    ) {
        resultsService.create(session.id(), eventId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId
    ) {
        resultsService.delete(session.id(), eventId);
        return ResponseEntity.noContent().build();
    }
}
