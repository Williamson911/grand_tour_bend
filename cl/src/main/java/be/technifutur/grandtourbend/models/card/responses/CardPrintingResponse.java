package be.technifutur.grandtourbend.models.card.responses;

import java.util.UUID;

public record CardPrintingResponse(
        UUID cardId,
        UUID variantId,
        String name,
        String backName,
        String cardType,
        String color,
        String cardNumber,
        String series,
        String rarity,
        String imgLink
) {
}
