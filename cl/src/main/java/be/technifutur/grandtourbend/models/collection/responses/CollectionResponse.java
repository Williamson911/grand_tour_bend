package be.technifutur.grandtourbend.models.collection.responses;

import java.util.List;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        String name,
        List<CollectionItemResponse> items
) {
}
