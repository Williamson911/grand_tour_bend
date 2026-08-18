package be.technifutur.grandtourbend.seeds;

import java.util.List;

public record CardSeedEntry(CardSeedData card, List<CardVariantSeedData> variants) {

    public record CardSeedData(
            Integer sourceId,
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
    ) {}

    public record CardVariantSeedData(
            Integer sourceId,
            String cardNumber,
            String series,
            String rarity,
            String imgLink,
            List<String> finishes,
            boolean isBanned,
            boolean isLimited,
            boolean hasErrata,
            Integer limitedTo,
            Integer viewCount
    ) {}
}
