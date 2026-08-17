package be.technifutur.grandtourbend.models.auth.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(

        @NotBlank
        String username,

        @NotBlank
        @Email
        String email,

        String bandaiTcgId
) {
}
