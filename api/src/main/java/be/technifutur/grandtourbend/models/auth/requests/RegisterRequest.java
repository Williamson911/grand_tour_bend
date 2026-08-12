package be.technifutur.grandtourbend.models.auth.requests;

import be.technifutur.grandtourbend.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(

        @NotBlank
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {

    public User toUser() {
        return new User(username, email, password);
    }
}
