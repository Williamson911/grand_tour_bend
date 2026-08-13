package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.models.results.responses.ResultsResponse;

import java.util.List;
import java.util.UUID;

public interface ResultsService {
    List<ResultsResponse> getAll(UUID userId);
    UUID create(UUID userId, UUID eventId, ResultsRequest request);
    void delete(UUID userId, UUID eventId);
}
