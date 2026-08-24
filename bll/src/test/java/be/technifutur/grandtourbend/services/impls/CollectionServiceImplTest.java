package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.entities.Collection;
import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.VariantNotOwnedByCardException;
import be.technifutur.grandtourbend.models.collection.requests.CollectionItemRequest;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import be.technifutur.grandtourbend.repositories.CollectionCardRepository;
import be.technifutur.grandtourbend.repositories.CollectionRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceImplTest {

    @Mock private CollectionRepository collectionRepository;
    @Mock private CollectionCardRepository collectionCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardVariantRepository cardVariantRepository;

    @InjectMocks
    private CollectionServiceImpl collectionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    private Card card() {
        Card card = new Card();
        card.setCardNumber("BT18-030");
        card.setName("Son Goku");
        return card;
    }

    @Test
    void create_withValidItems_savesCollectionAndItems() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> {
            Collection c = inv.getArgument(0);
            return c;
        });
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 3, BigDecimal.valueOf(12.5), null))
        );

        CollectionResponse response = collectionService.create(userId, request);

        assertThat(response.name()).isEqualTo("Ma collection");
        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getFirst().getPrice()).isEqualByComparingTo("12.5");
    }

    @Test
    void create_withLanguageSet_savesCollectionCardWithLanguage() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 3, BigDecimal.valueOf(12.5), "FR"))
        );

        collectionService.create(userId, request);

        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getLanguage()).isEqualTo("FR");
    }

    @Test
    void create_withoutLanguage_savesCollectionCardWithNullLanguage() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 3, BigDecimal.valueOf(12.5), null))
        );

        collectionService.create(userId, request);

        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getLanguage()).isNull();
    }

    @Test
    void create_whenCardDoesNotExist_throwsCardNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, null, 1, BigDecimal.ONE, null))
        );

        assertThatThrownBy(() -> collectionService.create(userId, request))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void create_whenVariantDoesNotBelongToCard_throwsVariantNotOwnedByCardException() {
        UUID otherCardId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        // UuidBaseEntity.id has no public setter (only @Getter, populated via
        // @GeneratedValue in real use), so a mock is the only way to give this
        // Card a specific id in a unit test.
        Card otherCard = org.mockito.Mockito.mock(Card.class);
        when(otherCard.getId()).thenReturn(otherCardId);
        CardVariant variant = new CardVariant();
        variant.setCard(otherCard);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(cardVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        CollectionRequest request = new CollectionRequest(
                "Ma collection",
                List.of(new CollectionItemRequest(cardId, variantId, 1, BigDecimal.ONE, null))
        );

        assertThatThrownBy(() -> collectionService.create(userId, request))
                .isInstanceOf(VariantNotOwnedByCardException.class);
    }

    @Test
    void update_replacesExistingItemsWithTheNewSet() {
        UUID collectionId = UUID.randomUUID();
        Collection existing = new Collection(new User(), "Old name");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card()));
        when(collectionCardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(collectionCardRepository.findByCollection_Id(any())).thenReturn(List.of());

        CollectionRequest request = new CollectionRequest(
                "New name",
                List.of(new CollectionItemRequest(cardId, null, 2, BigDecimal.TEN, null))
        );

        CollectionResponse response = collectionService.update(userId, collectionId, request);

        assertThat(response.name()).isEqualTo("New name");
        org.mockito.Mockito.verify(collectionCardRepository).deleteAllByCollection_Id(collectionId);
        assertThat(existing.getName()).isEqualTo("New name");

        ArgumentCaptor<List<CollectionCard>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(collectionCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getFirst().getPrice()).isEqualByComparingTo("10");
    }

    @Test
    void update_whenCollectionNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        CollectionRequest request = new CollectionRequest("Name", List.of());

        assertThatThrownBy(() -> collectionService.update(userId, collectionId, request))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }

    @Test
    void delete_removesCollectionAndItsItems() {
        UUID collectionId = UUID.randomUUID();
        Collection existing = new Collection(new User(), "Name");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(existing));

        collectionService.delete(userId, collectionId);

        org.mockito.Mockito.verify(collectionCardRepository).deleteAllByCollection_Id(collectionId);
        org.mockito.Mockito.verify(collectionRepository).delete(existing);
    }

    @Test
    void delete_whenCollectionNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.delete(userId, collectionId))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }

    @Test
    void getAll_returnsSummariesWithCardCountAndTotalPrice() {
        Collection collection = new Collection(new User(), "Ma collection");
        when(collectionRepository.findByUser_Id(userId)).thenReturn(List.of(collection));
        when(collectionCardRepository.sumQuantityByCollection_Id(collection.getId())).thenReturn(5L);
        when(collectionCardRepository.sumTotalPriceByCollection_Id(collection.getId())).thenReturn(BigDecimal.valueOf(42.5));
        when(collectionCardRepository.findTopByCollection_IdOrderByPriceDesc(collection.getId())).thenReturn(Optional.empty());

        List<CollectionSummaryResponse> result = collectionService.getAll(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Ma collection");
        assertThat(result.getFirst().cardCount()).isEqualTo(5L);
        assertThat(result.getFirst().totalPrice()).isEqualByComparingTo("42.5");
    }

    @Test
    void getAll_whenCollectionHasItems_returnsThumbnailImgLinkFromMostExpensiveCard() {
        Collection collection = new Collection(new User(), "Ma collection");
        Card card = card();
        card.setImgLink("https://example.com/card.png");
        CollectionCard topCard = new CollectionCard();
        topCard.setCard(card);
        topCard.setVariant(null);
        topCard.setQuantity(1);
        topCard.setPrice(BigDecimal.valueOf(99.99));

        when(collectionRepository.findByUser_Id(userId)).thenReturn(List.of(collection));
        when(collectionCardRepository.sumQuantityByCollection_Id(collection.getId())).thenReturn(1L);
        when(collectionCardRepository.sumTotalPriceByCollection_Id(collection.getId())).thenReturn(BigDecimal.valueOf(99.99));
        when(collectionCardRepository.findTopByCollection_IdOrderByPriceDesc(collection.getId())).thenReturn(Optional.of(topCard));

        List<CollectionSummaryResponse> result = collectionService.getAll(userId);

        assertThat(result.getFirst().thumbnailImgLink()).isEqualTo("https://example.com/card.png");
    }

    @Test
    void getAll_whenCollectionIsEmpty_returnsNullThumbnailImgLink() {
        Collection collection = new Collection(new User(), "Ma collection");
        when(collectionRepository.findByUser_Id(userId)).thenReturn(List.of(collection));
        when(collectionCardRepository.sumQuantityByCollection_Id(collection.getId())).thenReturn(0L);
        when(collectionCardRepository.sumTotalPriceByCollection_Id(collection.getId())).thenReturn(BigDecimal.ZERO);
        when(collectionCardRepository.findTopByCollection_IdOrderByPriceDesc(collection.getId())).thenReturn(Optional.empty());

        List<CollectionSummaryResponse> result = collectionService.getAll(userId);

        assertThat(result.getFirst().thumbnailImgLink()).isNull();
    }

    @Test
    void getAll_whenTopCardHasVariant_returnsVariantImgLinkOverCardImgLink() {
        Collection collection = new Collection(new User(), "Ma collection");
        Card card = card();
        card.setImgLink("https://example.com/card.png");
        CardVariant variant = new CardVariant();
        variant.setCard(card);
        variant.setImgLink("https://example.com/variant.png");
        CollectionCard topCard = new CollectionCard();
        topCard.setCard(card);
        topCard.setVariant(variant);
        topCard.setQuantity(1);
        topCard.setPrice(BigDecimal.valueOf(150));

        when(collectionRepository.findByUser_Id(userId)).thenReturn(List.of(collection));
        when(collectionCardRepository.sumQuantityByCollection_Id(collection.getId())).thenReturn(1L);
        when(collectionCardRepository.sumTotalPriceByCollection_Id(collection.getId())).thenReturn(BigDecimal.valueOf(150));
        when(collectionCardRepository.findTopByCollection_IdOrderByPriceDesc(collection.getId())).thenReturn(Optional.of(topCard));

        List<CollectionSummaryResponse> result = collectionService.getAll(userId);

        assertThat(result.getFirst().thumbnailImgLink()).isEqualTo("https://example.com/variant.png");
    }

    @Test
    void getById_whenOwnedByUser_returnsFullDetail() {
        UUID collectionId = UUID.randomUUID();
        Collection collection = new Collection(new User(), "Ma collection");
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.of(collection));
        when(collectionCardRepository.findByCollection_Id(collection.getId())).thenReturn(List.of());

        CollectionResponse result = collectionService.getById(userId, collectionId);

        assertThat(result.name()).isEqualTo("Ma collection");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getById_whenNotOwnedByUser_throwsCollectionNotFoundException() {
        UUID collectionId = UUID.randomUUID();
        when(collectionRepository.findByIdAndUser_Id(collectionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getById(userId, collectionId))
                .isInstanceOf(be.technifutur.grandtourbend.exceptions.CollectionNotFoundException.class);
    }
}
