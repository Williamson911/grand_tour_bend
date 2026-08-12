package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(name = "expenses")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Expenses extends  BaseEntity<UUID> {
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "event_id", nullable = false)
    @Getter
    private Event event;

    @Column(nullable = false)
    @Getter @Setter
    private String category;

    @Column(nullable = false)
    @Getter @Setter
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Getter @Setter
    private String currency;

    @Column
    @Getter @Setter
    private String notes;
}
