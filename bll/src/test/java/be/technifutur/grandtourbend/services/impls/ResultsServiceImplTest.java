package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.InvalidLeaderCardTypeException;
import be.technifutur.grandtourbend.models.results.requests.ResultsRequest;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.EventRepository;
import be.technifutur.grandtourbend.repositories.ResultsRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultsServiceImplTest {

    @Mock private ResultsRepository resultsRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CardRepository cardRepository;

    @InjectMocks
    private ResultsServiceImpl resultsService;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID leaderCardId = UUID.randomUUID();

    private ResultsRequest request() {
        return new ResultsRequest("My Deck", leaderCardId, 1, 10, BigDecimal.TEN, null, List.of());
    }

    @Test
    void create_whenLeaderCardIsValid_savesResultsWithLeaderCardAttached() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(new Event()));

        Card leaderCard = new Card();
        leaderCard.setCardNumber("BT18-030");
        leaderCard.setCardType("LEADER");
        when(cardRepository.findById(leaderCardId)).thenReturn(Optional.of(leaderCard));

        when(resultsRepository.findByUser_IdAndEvent_Id(userId, eventId)).thenReturn(Optional.empty());
        when(resultsRepository.save(any(Results.class))).thenAnswer(invocation -> invocation.getArgument(0));

        resultsService.create(userId, eventId, request());

        ArgumentCaptor<Results> captor = ArgumentCaptor.forClass(Results.class);
        verify(resultsRepository).save(captor.capture());
        assertThat(captor.getValue().getLeaderCard()).isEqualTo(leaderCard);
        assertThat(captor.getValue().getDeckName()).isEqualTo("My Deck");
    }

    @Test
    void create_whenLeaderCardIsNotALeaderType_throwsInvalidLeaderCardTypeException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(new Event()));

        Card battleCard = new Card();
        battleCard.setCardNumber("BT18-050");
        battleCard.setCardType("BATTLE");
        when(cardRepository.findById(leaderCardId)).thenReturn(Optional.of(battleCard));

        assertThatThrownBy(() -> resultsService.create(userId, eventId, request()))
                .isInstanceOf(InvalidLeaderCardTypeException.class);
    }

    @Test
    void create_whenLeaderCardDoesNotExist_throwsCardNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(new Event()));
        when(cardRepository.findById(leaderCardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultsService.create(userId, eventId, request()))
                .isInstanceOf(CardNotFoundException.class);
    }
}
