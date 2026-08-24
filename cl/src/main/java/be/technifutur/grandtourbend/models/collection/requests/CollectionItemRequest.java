package be.technifutur.grandtourbend.models.collection.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionItemRequest(
        @NotNull UUID cardId,
        UUID variantId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0") BigDecimal price
) {
}
