package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Role;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.repositories.RoleRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    new Role("USER"),
                    new Role("ADMIN")
            ));
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

            User admin = new User("admin", "admin@grandtourbend.local", passwordEncoder.encode("admin123"));
            admin.getRoles().add(adminRole);
            admin.setConfirmed(true);

            userRepository.save(admin);
        }
    }
}
