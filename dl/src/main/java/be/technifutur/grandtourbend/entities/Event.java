package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Event extends UuidBaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    @Getter @Setter
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id", nullable = false)
    @Getter @Setter
    private EventType type;

    @Column(nullable = false)
    @Getter @Setter
    private LocalDate date;

    @Embedded
    @Getter @Setter
    private Address address;

    @Column(name = "register_link")
    @Getter @Setter
    private String registerLink;

}
