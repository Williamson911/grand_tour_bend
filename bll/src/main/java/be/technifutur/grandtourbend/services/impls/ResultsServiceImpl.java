package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.ResultsService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.EventNotFoundException;
import be.technifutur.grandtourbend.exceptions.InvalidLeaderCardTypeException;
import be.technifutur.grandtourbend.exceptions.ResultsNotFoundException;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.models.results.responses.ResultsResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.ResultsRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultsServiceImpl implements ResultsService {

    private static final Set<String> LEADER_CARD_TYPES = Set.of("LEADER", "Z-LEADER");

    private final ResultsRepository resultsRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CardRepository cardRepository;

    @Override
    public List<ResultsResponse> getAll(UUID userId) {
        return resultsRepository.findByUser_Id(userId)
                .stream()
                .map(ResultsResponse::fromResults)
                .toList();
    }

    @Override
    public UUID create(UUID userId, UUID eventId, ResultsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new EventNotFoundException("Event with id " + eventId + " not found")
        );

        Card leaderCard = cardRepository.findById(request.leaderCardId()).orElseThrow(() ->
                new CardNotFoundException("Card with id " + request.leaderCardId() + " not found")
        );

        if (!LEADER_CARD_TYPES.contains(leaderCard.getCardType())) {
            throw new InvalidLeaderCardTypeException(
                    "Card " + leaderCard.getCardNumber() + " is not a leader card (type: " + leaderCard.getCardType() + ")"
            );
        }

        Results results = resultsRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .map(existing -> {
                    request.applyTo(existing, leaderCard);
                    return existing;
                })
                .orElseGet(() -> request.toResults(user, event, leaderCard));

        return resultsRepository.save(results).getId();
    }

    @Override
    public void delete(UUID userId, UUID eventId) {
        Results results = resultsRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .orElseThrow(() -> new ResultsNotFoundException("No result for this event"));

        resultsRepository.delete(results);
    }
}
