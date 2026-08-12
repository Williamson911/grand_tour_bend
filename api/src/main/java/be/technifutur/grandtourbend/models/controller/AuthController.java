package be.technifutur.grandtourbend.models.controller;

import be.technifutur.grandtourbend.models.auth.requests.LoginRequest;
import be.technifutur.grandtourbend.models.auth.requests.RegisterRequest;
import be.technifutur.grandtourbend.models.auth.responses.AuthResponse;
import be.technifutur.grandtourbend.models.auth.responses.RegisterResponse;
import be.technifutur.grandtourbend.services.security.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UUID id = authService.register(request.toUser());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(id, request.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request.username(), request.password());

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
