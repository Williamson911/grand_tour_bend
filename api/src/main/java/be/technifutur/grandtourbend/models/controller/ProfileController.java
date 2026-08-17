package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.models.auth.requests.UpdateProfileRequest;
import be.technifutur.grandtourbend.models.auth.responses.MeResponse;
import be.technifutur.grandtourbend.services.security.AuthService;
import be.technifutur.grandtourbend.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profil de l'utilisateur connecté")
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Récupérer mon profil")
    @ApiResponse(responseCode = "200", description = "Profil de l'utilisateur connecté")
    public ResponseEntity<MeResponse> me(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        return ResponseEntity.ok(MeResponse.fromUser(authService.getProfile(session.id())));
    }

    @PutMapping("/profile")
    @Operation(summary = "Modifier mon profil", description = "Nom d'utilisateur, email et identifiant Bandai TCG+.")
    @ApiResponse(responseCode = "200", description = "Profil mis à jour")
    @ApiResponse(responseCode = "409", description = "Nom d'utilisateur ou email déjà pris")
    public ResponseEntity<MeResponse> update(
            @AuthenticationPrincipal JwtUtils.UserSession session,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        var updated = authService.updateProfile(
                session.id(),
                request.username(),
                request.email(),
                request.bandaiTcgId()
        );
        return ResponseEntity.ok(MeResponse.fromUser(updated));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Supprimer mon compte", description = "Supprime le compte et toutes les données associées (inscriptions, dépenses, résultats).")
    @ApiResponse(responseCode = "204", description = "Compte supprimé")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtUtils.UserSession session
    ) {
        authService.deleteAccount(session.id());
        return ResponseEntity.noContent().build();
    }
}
