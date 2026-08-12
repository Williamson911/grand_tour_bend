package be.technifutur.grandtourbend.entities;

import be.technifutur.grandtourbend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
public class Event {

    @Id
    @Getter
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    @Getter @Setter
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Getter
    private OffsetDateTime updatedAt;

    public Event(String name, EventType type, LocalDate date, Address address, String registerLink) {
        this.name = name;
        this.type = type;
        this.date = date;
        this.address = address;
        this.registerLink = registerLink;
    }

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
