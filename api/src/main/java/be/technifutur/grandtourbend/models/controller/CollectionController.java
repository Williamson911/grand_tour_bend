package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.CollectionService;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collections")
@CrossOrigin("*")
@Tag(name = "Collections", description = "Collections de cartes possédées par l'utilisateur connecté")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "Lister mes collections")
    @ApiResponse(responseCode = "200", description = "Liste des collections")
    public ResponseEntity<List<CollectionSummaryResponse>> getAll(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(collectionService.getAll(session.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une collection")
    @ApiResponse(responseCode = "200", description = "Détail de la collection")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<CollectionResponse> getById(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(collectionService.getById(session.id(), id));
    }

    @PostMapping
    @Operation(summary = "Créer une collection")
    @ApiResponse(responseCode = "201", description = "Collection créée")
    public ResponseEntity<CollectionResponse> create(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @Valid @RequestBody CollectionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.create(session.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Remplacer le nom et le contenu d'une collection")
    @ApiResponse(responseCode = "200", description = "Collection mise à jour")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<CollectionResponse> update(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request
    ) {
        return ResponseEntity.ok(collectionService.update(session.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une collection")
    @ApiResponse(responseCode = "204", description = "Collection supprimée")
    @ApiResponse(responseCode = "404", description = "Collection introuvable")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        collectionService.delete(session.id(), id);
        return ResponseEntity.noContent().build();
    }
}
