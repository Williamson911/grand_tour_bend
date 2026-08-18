package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private Card leaderCard() {
        Card card = new Card();
        card.setSourceId(1);
        card.setCardNumber("BT18-030");
        card.setName("Son Goku");
        card.setCardType("LEADER");
        card.setRarity("Uncommon[UC]");
        card.setSeries("BT18");
        return card;
    }

    @Test
    void getAll_withTypeFilter_delegatesToFindByCardType() {
        when(cardRepository.findByCardType(eq("LEADER"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll("LEADER", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().cardType()).isEqualTo("LEADER");
        verify(cardRepository).findByCardType("LEADER", pageable);
        verify(cardRepository, never()).findAll(any(Pageable.class));
        verify(cardRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        verify(cardRepository, never()).findByCardTypeAndNameContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withSearchFilter_delegatesToFindByNameContainingIgnoreCase() {
        when(cardRepository.findByNameContainingIgnoreCase(eq("search"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll(null, "search", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findByNameContainingIgnoreCase("search", pageable);
        verify(cardRepository, never()).findAll(any(Pageable.class));
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).findByCardTypeAndNameContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withTypeAndSearchFilters_delegatesToFindByCardTypeAndNameContainingIgnoreCase() {
        when(cardRepository.findByCardTypeAndNameContainingIgnoreCase(eq("LEADER"), eq("Goku"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll("LEADER", "Goku", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findByCardTypeAndNameContainingIgnoreCase("LEADER", "Goku", pageable);
        verify(cardRepository, never()).findAll(any(Pageable.class));
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withNoFilters_delegatesToFindAll() {
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findAll(pageable);
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        verify(cardRepository, never()).findByCardTypeAndNameContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getById_whenFound_returnsCardDetailResponse() {
        UUID id = UUID.randomUUID();
        Card card = leaderCard();
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        CardDetailResponse result = cardService.getById(id);

        assertThat(result).isNotNull();
        assertThat(result.card()).isNotNull();
        assertThat(result.card().name()).isEqualTo("Son Goku");
        assertThat(result.card().cardType()).isEqualTo("LEADER");
        assertThat(result.card().cardNumber()).isEqualTo("BT18-030");
        assertThat(result.variants()).isEmpty();
        verify(cardRepository).findById(id);
    }

    @Test
    void getById_whenMissing_throwsCardNotFoundException() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        CardNotFoundException exception = catchThrowableOfType(
                () -> cardService.getById(id), CardNotFoundException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getError()).asString().contains(id.toString());
    }
}
