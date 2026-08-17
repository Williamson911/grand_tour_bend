package be.technifutur.grandtourbend.services.security;

import be.technifutur.grandtourbend.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface AuthService extends UserDetailsService {

    UUID register(User user);

    String login(String username, String password);

    String confirm(String token);

    void resendConfirmation(String email);

    User getProfile(UUID id);

    User updateProfile(UUID id, String username, String email, String bandaiTcgId);

    void deleteAccount(UUID id);

    void requestPasswordReset(String email);

    void resetPassword(String token, String newPassword);
}
