package be.technifutur.grandtourbend.models.card.responses;

import java.util.List;

public record CardFacetsResponse(
        List<String> colors,
        List<String> series
) {
}
