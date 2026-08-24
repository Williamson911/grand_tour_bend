package be.technifutur.grandtourbend.models.collection.responses;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionSummaryResponse(
        UUID id,
        String name,
        long cardCount,
        BigDecimal totalPrice
) {
}
