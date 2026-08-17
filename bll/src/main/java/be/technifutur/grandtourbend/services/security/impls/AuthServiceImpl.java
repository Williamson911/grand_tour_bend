package be.technifutur.grandtourbend.services.security.impls;

import be.technifutur.grandtourbend.entities.Role;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.EmailAlreadyExistsException;
import be.technifutur.grandtourbend.exceptions.EmailNotConfirmedException;
import be.technifutur.grandtourbend.exceptions.InvalidConfirmationTokenException;
import be.technifutur.grandtourbend.exceptions.InvalidCredentialsException;
import be.technifutur.grandtourbend.exceptions.InvalidResetTokenException;
import be.technifutur.grandtourbend.exceptions.UserNotFoundException;
import be.technifutur.grandtourbend.exceptions.UsernameAlreadyExistsException;
import be.technifutur.grandtourbend.repositories.ExpensesRepository;
import be.technifutur.grandtourbend.repositories.RegistrationRepository;
import be.technifutur.grandtourbend.repositories.ResultsRepository;
import be.technifutur.grandtourbend.repositories.RoleRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import be.technifutur.grandtourbend.services.security.AuthService;
import be.technifutur.grandtourbend.utils.JwtUtils;
import be.technifutur.grandtourbend.utils.MailerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RegistrationRepository registrationRepository;
    private final ExpensesRepository expensesRepository;
    private final ResultsRepository resultsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final MailerUtils mailerUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email " + user.getEmail() + " already exists");
        }

        Role userRole = roleRepository.findByName("USER").orElseThrow(() ->
                new IllegalStateException("Role USER not seeded")
        );

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(userRole);
        user.setConfirmed(false);
        user.setConfirmationToken(UUID.randomUUID().toString());

        User saved = userRepository.save(user);

        sendConfirmationEmail(saved);

        return saved.getId();
    }

    @Override
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new InvalidCredentialsException("Invalid email or password")
        );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isConfirmed()) {
            throw new EmailNotConfirmedException();
        }

        return jwtUtils.generateToken(user);
    }

    @Override
    public String confirm(String token) {
        User user = userRepository.findByConfirmationToken(token).orElseThrow(
                InvalidConfirmationTokenException::new
        );

        user.setConfirmed(true);
        user.setConfirmationToken(null);
        userRepository.save(user);

        return user.getUsername();
    }

    @Override
    public void resendConfirmation(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        if (user.isConfirmed()) {
            return;
        }

        user.setConfirmationToken(UUID.randomUUID().toString());
        userRepository.save(user);

        sendConfirmationEmail(user);
    }

    @Override
    public User getProfile(UUID id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User updateProfile(UUID id, String username, String email, String bandaiTcgId) {
        User user = getProfile(id);

        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new UsernameAlreadyExistsException("Username " + username + " already exists");
        }
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException("Email " + email + " already exists");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setBandaiTcgId(bandaiTcgId);

        return userRepository.save(user);
    }

    @Override
    public void deleteAccount(UUID id) {
        registrationRepository.deleteAllByUser_Id(id);
        expensesRepository.deleteAllByUser_Id(id);
        resultsRepository.deleteAllByUser_Id(id);
        userRepository.deleteById(id);
    }

    @Override
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPasswordResetToken(UUID.randomUUID().toString());
            userRepository.save(user);

            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable(
                    "resetLink",
                    frontendUrl + "/auth/reset?token=" + user.getPasswordResetToken()
            );

            Thread thread = new Thread(mailerUtils.createThread(
                    "Réinitialise ton mot de passe Grand Tour DBSCG",
                    "reset-password",
                    context,
                    user.getEmail()
            ));
            thread.start();
        });
        // Always returns silently, whether the email exists or not, to avoid enumeration.
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token).orElseThrow(
                InvalidResetTokenException::new
        );

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    private void sendConfirmationEmail(User user) {
        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable(
                "confirmLink",
                frontendUrl + "/auth/verified?token=" + user.getConfirmationToken()
        );

        Thread thread = new Thread(mailerUtils.createThread(
                "Confirme ton compte Grand Tour DBSCG",
                "confirm-account",
                context,
                user.getEmail()
        ));
        thread.start();
    }
}
