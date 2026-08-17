package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.models.auth.requests.ForgotPasswordRequest;
import be.technifutur.grandtourbend.models.auth.requests.LoginRequest;
import be.technifutur.grandtourbend.models.auth.requests.RegisterRequest;
import be.technifutur.grandtourbend.models.auth.requests.ResendConfirmationRequest;
import be.technifutur.grandtourbend.models.auth.requests.ResetPasswordRequest;
import be.technifutur.grandtourbend.models.auth.responses.AuthResponse;
import be.technifutur.grandtourbend.models.auth.responses.ConfirmResponse;
import be.technifutur.grandtourbend.models.auth.responses.RegisterResponse;
import be.technifutur.grandtourbend.services.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        String token = authService.login(request.email(), request.password());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/confirm")
    @Operation(summary = "Confirmer un compte", description = "Valide le token reçu par email et active le compte.")
    @ApiResponse(responseCode = "200", description = "Compte confirmé")
    @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    public ResponseEntity<ConfirmResponse> confirm(@RequestParam String token) {
        String username = authService.confirm(token);

        return ResponseEntity.ok(new ConfirmResponse(username));
    }

    @PostMapping("/resend-confirmation")
    @Operation(summary = "Renvoyer l'email de confirmation")
    @ApiResponse(responseCode = "200", description = "Email renvoyé (si le compte existe et n'est pas déjà confirmé)")
    public ResponseEntity<Void> resendConfirmation(
            @Valid @RequestBody ResendConfirmationRequest request
    ) {
        authService.resendConfirmation(request.email());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander une réinitialisation de mot de passe")
    @ApiResponse(responseCode = "200", description = "Email envoyé si le compte existe (pas d'énumération)")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.requestPasswordReset(request.email());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser mon mot de passe", description = "Consomme le token reçu par email.")
    @ApiResponse(responseCode = "200", description = "Mot de passe mis à jour")
    @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.ok().build();
    }
}
