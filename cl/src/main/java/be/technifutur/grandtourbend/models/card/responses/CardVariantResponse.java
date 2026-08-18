package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.CardVariant;

import java.util.List;
import java.util.UUID;

public record CardVariantResponse(
        UUID id,
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
) {
    public static CardVariantResponse fromCardVariant(CardVariant v) {
        return new CardVariantResponse(
                v.getId(),
                v.getCardNumber(),
                v.getSeries(),
                v.getRarity(),
                v.getImgLink(),
                v.getFinishes(),
                v.isBanned(),
                v.isLimited(),
                v.isHasErrata(),
                v.getLimitedTo(),
                v.getViewCount()
        );
    }
}
