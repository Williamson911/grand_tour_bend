package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardFacetsResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardPrintingProjection;
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
        verify(cardRepository, never()).searchByNameOrBackName(anyString(), any(Pageable.class));
        verify(cardRepository, never()).searchByCardTypeAndNameOrBackName(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withSearchFilter_delegatesToSearchByNameOrBackName() {
        when(cardRepository.searchByNameOrBackName(eq("search"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll(null, "search", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).searchByNameOrBackName("search", pageable);
        verify(cardRepository, never()).findAll(any(Pageable.class));
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).searchByCardTypeAndNameOrBackName(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withSearchFilter_matchesCardsByAwakenedBackNameToo() {
        Card godSonGoku = leaderCard();
        godSonGoku.setBackName("SS4 Son Goku, Guardian of History");
        when(cardRepository.searchByNameOrBackName(eq("Guardian of History"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(godSonGoku)));

        Page<CardResponse> result = cardService.getAll(null, "Guardian of History", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().backName()).isEqualTo("SS4 Son Goku, Guardian of History");
    }

    @Test
    void getAll_withTypeAndSearchFilters_delegatesToSearchByCardTypeAndNameOrBackName() {
        when(cardRepository.searchByCardTypeAndNameOrBackName(eq("LEADER"), eq("Goku"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll("LEADER", "Goku", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).searchByCardTypeAndNameOrBackName("LEADER", "Goku", pageable);
        verify(cardRepository, never()).findAll(any(Pageable.class));
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).searchByNameOrBackName(anyString(), any(Pageable.class));
    }

    @Test
    void getAll_withNoFilters_delegatesToFindAll() {
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(leaderCard())));

        Page<CardResponse> result = cardService.getAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findAll(pageable);
        verify(cardRepository, never()).findByCardType(anyString(), any(Pageable.class));
        verify(cardRepository, never()).searchByNameOrBackName(anyString(), any(Pageable.class));
        verify(cardRepository, never()).searchByCardTypeAndNameOrBackName(anyString(), anyString(), any(Pageable.class));
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

    @Test
    void getPrintings_delegatesToFindPrintingsWithAllFilters() {
        CardPrintingProjection projection = new CardPrintingProjection() {
            public UUID getCardId() { return UUID.fromString("00000000-0000-0000-0000-000000000001"); }
            public UUID getVariantId() { return null; }
            public String getName() { return "Son Goku"; }
            public String getBackName() { return null; }
            public String getCardType() { return "LEADER"; }
            public String getColor() { return "Red"; }
            public String getCardNumber() { return "BT18-030"; }
            public String getSeries() { return "BT18"; }
            public String getRarity() { return "Common[C]"; }
            public String getImgLink() { return "BT18-030"; }
        };
        when(cardRepository.findPrintings(eq("LEADER"), eq("Goku"), eq("Red"), eq("BT18"), eq("Common[C]"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        Page<CardPrintingResponse> result = cardService.getPrintings("LEADER", "Goku", "Red", "BT18", "Common[C]", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().cardNumber()).isEqualTo("BT18-030");
        assertThat(result.getContent().getFirst().variantId()).isNull();
        verify(cardRepository).findPrintings("LEADER", "Goku", "Red", "BT18", "Common[C]", pageable);
    }

    @Test
    void getFacets_returnsDistinctColorsSeriesAndRaritiesFromRepository() {
        when(cardRepository.findDistinctColors()).thenReturn(List.of("Blue", "Red"));
        when(cardRepository.findDistinctSeries()).thenReturn(List.of("BT1", "BT2"));
        when(cardRepository.findDistinctRarities()).thenReturn(List.of("Common[C]", "Super Rare[SR]"));

        CardFacetsResponse result = cardService.getFacets();

        assertThat(result.colors()).containsExactly("Blue", "Red");
        assertThat(result.series()).containsExactly("BT1", "BT2");
        assertThat(result.rarities()).containsExactly("Common[C]", "Super Rare[SR]");
        verify(cardRepository).findDistinctColors();
        verify(cardRepository).findDistinctSeries();
        verify(cardRepository).findDistinctRarities();
    }
}
