package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_")
@NoArgsConstructor @AllArgsConstructor
@ToString(callSuper = true, of = "name") @EqualsAndHashCode(callSuper = true, of = "name")
public class Role extends BaseEntity<Long> {

    @Column(unique = true, nullable = false, length = 50)
    @Getter @Setter
    private String name;
}
