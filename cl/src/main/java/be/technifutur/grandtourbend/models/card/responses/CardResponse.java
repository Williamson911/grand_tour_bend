package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.Card;

import java.util.List;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String cardNumber,
        String name,
        String cardType,
        String color,
        String energyCost,
        Integer zEnergyCost,
        Integer power,
        Integer comboCost,
        Integer comboPower,
        String skill,
        List<String> characters,
        List<String> traits,
        List<String> era,
        List<String> keywords,
        String rarity,
        String series,
        String imgLink,
        boolean isHorizontal,
        boolean isBanned,
        boolean isLimited,
        boolean hasErrata,
        Integer limitedTo,
        Integer viewCount,
        String backName,
        String backSkill,
        Integer backPower
) {
    public static CardResponse fromCard(Card c) {
        return new CardResponse(
                c.getId(),
                c.getCardNumber(),
                c.getName(),
                c.getCardType(),
                c.getColor(),
                c.getEnergyCost(),
                c.getZEnergyCost(),
                c.getPower(),
                c.getComboCost(),
                c.getComboPower(),
                c.getSkill(),
                c.getCharacters(),
                c.getTraits(),
                c.getEra(),
                c.getKeywords(),
                c.getRarity(),
                c.getSeries(),
                c.getImgLink(),
                c.isHorizontal(),
                c.isBanned(),
                c.isLimited(),
                c.isHasErrata(),
                c.getLimitedTo(),
                c.getViewCount(),
                c.getBackName(),
                c.getBackSkill(),
                c.getBackPower()
        );
    }
}
