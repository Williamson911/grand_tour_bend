package be.technifutur.grandtourbend.models.collection.responses;

import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionItemResponse(
        UUID cardId,
        UUID variantId,
        Integer quantity,
        BigDecimal price,
        String language,
        CardPrintingResponse card
) {
    public static CollectionItemResponse fromCollectionCard(CollectionCard cc) {
        var variant = cc.getVariant();
        var card = cc.getCard();
        UUID variantId = variant != null ? variant.getId() : null;
        CardPrintingResponse printing = new CardPrintingResponse(
                card.getId(),
                variantId,
                card.getName(),
                card.getBackName(),
                card.getCardType(),
                card.getColor(),
                variant != null ? variant.getCardNumber() : card.getCardNumber(),
                variant != null ? variant.getSeries() : card.getSeries(),
                variant != null ? variant.getRarity() : card.getRarity(),
                variant != null ? variant.getImgLink() : card.getImgLink()
        );
        return new CollectionItemResponse(card.getId(), variantId, cc.getQuantity(), cc.getPrice(), cc.getLanguage(), printing);
    }
}
