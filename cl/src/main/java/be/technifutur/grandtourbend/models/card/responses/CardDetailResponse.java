package be.technifutur.grandtourbend.models.card.responses;

import be.technifutur.grandtourbend.entities.Card;

import java.util.List;

public record CardDetailResponse(
        CardResponse card,
        List<CardVariantResponse> variants
) {
    public static CardDetailResponse fromCard(Card c) {
        return new CardDetailResponse(
                CardResponse.fromCard(c),
                c.getVariants().stream().map(CardVariantResponse::fromCardVariant).toList()
        );
    }
}
