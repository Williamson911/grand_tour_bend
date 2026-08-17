package be.technifutur.grandtourbend.models.auth.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendConfirmationRequest(

        @NotBlank
        @Email
        String email
) {
}
