package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collections")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Collection extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    private User user;

    @Column(nullable = false)
    @Getter
    @Setter
    private String name;
}
