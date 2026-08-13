package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.ExpensesService;
import be.technifutur.grandtourbend.models.expenses.requests.ExpenseRequest;
import be.technifutur.grandtourbend.models.expenses.responses.ExpensesResponse;
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
@RequestMapping("/expenses")
@CrossOrigin("*")
@Tag(name = "Expenses", description = "Dépenses liées à la participation à un event (inscription, hôtel, transport...)")
public class ExpensesController {
    private final ExpensesService expensesService;

    @GetMapping("/me")
    @Operation(summary = "Lister mes dépenses", description = "Renvoie toutes les dépenses de l'utilisateur connecté, tous events confondus.")
    @ApiResponse(responseCode = "200", description = "Liste des dépenses")
    public ResponseEntity<List<ExpensesResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(expensesService.getAll(session.id()));
    }

    @PostMapping("/{eventId}")
    @Operation(summary = "Ajouter une dépense pour un event")
    @ApiResponse(responseCode = "201", description = "Dépense créée")
    @ApiResponse(responseCode = "404", description = "Event introuvable")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId,
            @Valid @RequestBody ExpenseRequest request
    ) {
        expensesService.create(session.id(), eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une dépense", description = "Seul le propriétaire de la dépense peut la supprimer.")
    @ApiResponse(responseCode = "204", description = "Dépense supprimée")
    @ApiResponse(responseCode = "403", description = "La dépense appartient à un autre utilisateur")
    @ApiResponse(responseCode = "404", description = "Dépense introuvable")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        expensesService.delete(id, session.id());
        return ResponseEntity.noContent().build();
    }
}
