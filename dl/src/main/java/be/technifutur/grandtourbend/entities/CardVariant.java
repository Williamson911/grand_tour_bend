package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_variants")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CardVariant extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    @Getter @Setter
    private Card card;

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private Integer sourceId;

    @Column(nullable = false)
    @Getter @Setter
    private String cardNumber;

    @Column(nullable = false)
    @Getter @Setter
    private String series;

    @Column(nullable = false)
    @Getter @Setter
    private String rarity;

    @Column
    @Getter @Setter
    private String imgLink;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> finishes = new ArrayList<>();

    @Column(nullable = false)
    @Getter @Setter
    private boolean isBanned;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isLimited;

    @Column(nullable = false)
    @Getter @Setter
    private boolean hasErrata;

    @Column
    @Getter @Setter
    private Integer limitedTo;

    @Column
    @Getter @Setter
    private Integer viewCount;
}
