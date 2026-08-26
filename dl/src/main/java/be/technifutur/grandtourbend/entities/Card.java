package be.technifutur.grandtourbend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Card extends UuidBaseEntity {

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private Integer sourceId;

    @Column(nullable = false, unique = true)
    @Getter @Setter
    private String cardNumber;

    @Column(nullable = false)
    @Getter @Setter
    private String name;

    @Column(nullable = false)
    @Getter @Setter
    private String cardType;

    @Column
    @Getter @Setter
    private String color;

    @Column
    @Getter @Setter
    private String energyCost;

    @Column
    @Getter @Setter
    private Integer zEnergyCost;

    @Column
    @Getter @Setter
    private Integer power;

    @Column
    @Getter @Setter
    private Integer comboCost;

    @Column
    @Getter @Setter
    private Integer comboPower;

    @Column(columnDefinition = "text")
    @Getter @Setter
    private String skill;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> characters = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> traits = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> era = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Getter @Setter
    private List<String> keywords = new ArrayList<>();

    @Column
    @Getter @Setter
    private String rarity;

    @Column(nullable = false)
    @Getter @Setter
    private String series;

    @Column
    @Getter @Setter
    private String imgLink;

    // Deliberately NOT @Lob: on PostgreSQL, Hibernate maps a @Lob byte[] to
    // an "oid" column (large-object reference), which requires large-object
    // API access and throws "Unable to access lob stream" for plain
    // SELECT/INSERT usage outside that API. A plain byte[] column maps to
    // VARBINARY, which the PostgreSQL dialect stores as "bytea" — a normal
    // column that reads/writes like any other, which is what we want here.
    @Basic(fetch = FetchType.LAZY)
    @Getter @Setter
    private byte[] imageData;

    @Column
    @Getter @Setter
    private String imageContentType;

    @Column(nullable = false)
    @Getter @Setter
    private boolean isHorizontal;

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

    @Column
    @Getter @Setter
    private String backName;

    @Column(columnDefinition = "text")
    @Getter @Setter
    private String backSkill;

    @Column
    @Getter @Setter
    private Integer backPower;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter @Setter
    private List<CardVariant> variants = new ArrayList<>();
}
