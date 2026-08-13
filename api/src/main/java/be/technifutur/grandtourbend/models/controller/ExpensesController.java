package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.ExpensesService;
import be.technifutur.grandtourbend.models.expenses.requests.ExpenseRequest;
import be.technifutur.grandtourbend.models.expenses.responses.ExpensesResponse;
import be.technifutur.grandtourbend.utils.JwtUtils;
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
public class ExpensesController {
    private final ExpensesService expensesService;

    @GetMapping("/me")
    public ResponseEntity<List<ExpensesResponse>> getMine(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(expensesService.getAll(session.id()));
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID eventId,
            @Valid @RequestBody ExpenseRequest request
    ) {
        expensesService.create(session.id(), eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @PathVariable UUID id
    ) {
        expensesService.delete(id, session.id());
        return ResponseEntity.noContent().build();
    }
}
