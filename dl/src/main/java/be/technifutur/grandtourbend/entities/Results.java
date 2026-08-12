package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "results")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Results extends  BaseEntity<UUID> {
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "event_id", nullable = false)
    @Getter
    private Event event;

    @Column(nullable = false)
    @Getter
    @Setter
    private String deckName;


    @Column(nullable = false)
    @Getter
    @Setter
    private String leaderPlayed;

    @Column
    @Getter
    @Setter
    private Integer placement;

    @Column
    @Getter
    @Setter
    private Integer totalPlayers;

    @Column
    @Getter
    @Setter
    private BigDecimal prizes;

    @Column
    @Getter
    @Setter
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter
    @Setter
    private List<MatchResult> matches;
}