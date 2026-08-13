package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.models.auth.requests.LoginRequest;
import be.technifutur.grandtourbend.models.auth.requests.RegisterRequest;
import be.technifutur.grandtourbend.models.auth.responses.AuthResponse;
import be.technifutur.grandtourbend.models.auth.responses.RegisterResponse;
import be.technifutur.grandtourbend.services.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Inscription et connexion (génération du token JWT)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Créer un compte utilisateur")
    @ApiResponse(responseCode = "201", description = "Compte créé")
    @ApiResponse(responseCode = "409", description = "Nom d'utilisateur déjà pris")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UUID id = authService.register(request.toUser());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(id, request.username()));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter", description = "Renvoie un token JWT (valide 15 minutes) à utiliser dans le header Authorization: Bearer <token>.")
    @ApiResponse(responseCode = "200", description = "Token généré")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request.username(), request.password());

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
