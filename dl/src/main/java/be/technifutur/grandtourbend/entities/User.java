package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"username", "email", "password", "bandaiTcgId", "roles"})
@ToString(callSuper = true, exclude = "password")
public class User extends UuidBaseEntity implements UserDetails {

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private String username;

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private String email;

    @Column(nullable = false)
    @Getter @Setter
    private String password;

    @Column(name = "bandai_tcg_id")
    @Getter @Setter
    private String bandaiTcgId;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    @Getter @Setter
    private boolean confirmed = false;

    @Column(name = "confirmation_token")
    @Getter @Setter
    private String confirmationToken;

    @Column(name = "password_reset_token")
    @Getter @Setter
    private String passwordResetToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Getter
    private Set<Role> roles = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        }
        return authorities;
    }
}
