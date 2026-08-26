package be.technifutur.grandtourbend.seeds;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.repositories.CardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class CardsInitializer implements CommandLineRunner {

    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        if (cardRepository.count() > 0) {
            return;
        }

        List<CardSeedEntry> entries;
        try (InputStream in = new ClassPathResource("seed/dbs_cards.json").getInputStream()) {
            entries = objectMapper.readValue(
                    in,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CardSeedEntry.class)
            );
        }

        List<Card> cards = entries.stream().map(this::toCard).toList();
        cardRepository.saveAll(cards);
    }

    private Card toCard(CardSeedEntry entry) {
        CardSeedEntry.CardSeedData data = entry.card();

        Card card = new Card();
        card.setSourceId(data.sourceId());
        card.setCardNumber(data.cardNumber());
        card.setName(data.name());
        card.setCardType(data.cardType());
        card.setColor(data.color());
        card.setEnergyCost(data.energyCost());
        card.setZEnergyCost(data.zEnergyCost());
        card.setPower(data.power());
        card.setComboCost(data.comboCost());
        card.setComboPower(data.comboPower());
        card.setSkill(data.skill());
        card.setCharacters(data.characters());
        card.setTraits(data.traits());
        card.setEra(data.era());
        card.setKeywords(data.keywords());
        card.setRarity(data.rarity());
        card.setSeries(data.series());
        card.setImgLink(data.imgLink());
        card.setHorizontal(data.isHorizontal());
        card.setBanned(data.isBanned());
        card.setLimited(data.isLimited());
        card.setHasErrata(data.hasErrata());
        card.setLimitedTo(data.limitedTo());
        card.setViewCount(data.viewCount());
        card.setBackName(data.backName());
        card.setBackSkill(data.backSkill());
        card.setBackPower(data.backPower());

        for (CardSeedEntry.CardVariantSeedData v : entry.variants()) {
            CardVariant variant = new CardVariant();
            variant.setCard(card);
            variant.setSourceId(v.sourceId());
            variant.setCardNumber(v.cardNumber());
            variant.setSeries(v.series());
            variant.setRarity(v.rarity());
            variant.setImgLink(v.imgLink());
            variant.setFinishes(v.finishes());
            variant.setBanned(v.isBanned());
            variant.setLimited(v.isLimited());
            variant.setHasErrata(v.hasErrata());
            variant.setLimitedTo(v.limitedTo());
            variant.setViewCount(v.viewCount());
            card.getVariants().add(variant);
        }

        return card;
    }
}
