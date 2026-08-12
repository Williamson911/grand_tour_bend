package be.technifutur.grandtourbend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, of = {"name"})
@ToString(callSuper = true, of = {"name"})
public class EventType extends BaseEntity<Long> {

    @Column(nullable = false, unique = true, length = 50)
    @Getter
    @Setter
    private String name;
    @Column(nullable = false)
    @Getter
    @Setter
    private String icon;

    @Column(nullable = false)
    @Getter
    @Setter
    private String color;
}