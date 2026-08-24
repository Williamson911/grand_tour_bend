package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "collection_cards")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CollectionCard extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "collection_id", nullable = false)
    @Getter
    @Setter
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "card_id", nullable = false)
    @Getter
    @Setter
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "variant_id")
    @Getter
    @Setter
    private CardVariant variant;

    @Column(nullable = false)
    @Getter
    @Setter
    private Integer quantity;

    @Column(nullable = false)
    @Getter
    @Setter
    private BigDecimal price;

    @Column
    @Getter
    @Setter
    private String language;
}
