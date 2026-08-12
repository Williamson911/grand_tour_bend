package be.technifutur.grandtourbend.services.security;

import be.technifutur.grandtourbend.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface AuthService extends UserDetailsService {

    UUID register(User user);

    String login(String username, String password);
}
