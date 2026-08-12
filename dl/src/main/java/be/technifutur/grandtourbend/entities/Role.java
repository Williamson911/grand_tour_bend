package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_")
@NoArgsConstructor @AllArgsConstructor
@ToString @EqualsAndHashCode
public class Role {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @Getter @Setter
    private String name;

    public Role(String name) {
        this.name = name;
    }
}
