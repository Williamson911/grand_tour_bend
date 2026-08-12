package be.technifutur.grandtourbend.services.security.impls;

import be.technifutur.grandtourbend.entities.Role;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.InvalidCredentialsException;
import be.technifutur.grandtourbend.exceptions.UsernameAlreadyExistsException;
import be.technifutur.grandtourbend.repositories.RoleRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import be.technifutur.grandtourbend.services.security.AuthService;
import be.technifutur.grandtourbend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("No user " + username)
        );
    }

    @Override
    public UUID register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException("Username " + user.getUsername() + " already exists");
        }

        Role userRole = roleRepository.findByName("USER").orElseThrow(() ->
                new IllegalStateException("Role USER not seeded")
        );

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(userRole);

        return userRepository.save(user).getId();
    }

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new InvalidCredentialsException("Invalid username or password")
        );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return jwtUtils.generateToken(user);
    }
}
