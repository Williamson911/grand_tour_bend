package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionService {
    List<CollectionSummaryResponse> getAll(UUID userId);
    CollectionResponse getById(UUID userId, UUID collectionId);
    CollectionResponse create(UUID userId, CollectionRequest request);
    CollectionResponse update(UUID userId, UUID collectionId, CollectionRequest request);
    void delete(UUID userId, UUID collectionId);
}
