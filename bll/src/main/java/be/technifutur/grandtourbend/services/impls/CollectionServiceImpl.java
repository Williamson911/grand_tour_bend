package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.CollectionService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.CardVariant;
import be.technifutur.grandtourbend.entities.Collection;
import be.technifutur.grandtourbend.entities.CollectionCard;
import be.technifutur.grandtourbend.entities.User;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.exceptions.CollectionNotFoundException;
import be.technifutur.grandtourbend.exceptions.VariantNotOwnedByCardException;
import be.technifutur.grandtourbend.models.collection.requests.CollectionItemRequest;
import be.technifutur.grandtourbend.models.collection.requests.CollectionRequest;
import be.technifutur.grandtourbend.models.collection.responses.CollectionItemResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionResponse;
import be.technifutur.grandtourbend.models.collection.responses.CollectionSummaryResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import be.technifutur.grandtourbend.repositories.CardVariantRepository;
import be.technifutur.grandtourbend.repositories.CollectionCardRepository;
import be.technifutur.grandtourbend.repositories.CollectionRepository;
import be.technifutur.grandtourbend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionCardRepository collectionCardRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardVariantRepository cardVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CollectionSummaryResponse> getAll(UUID userId) {
        return collectionRepository.findByUser_Id(userId).stream()
                .map(c -> new CollectionSummaryResponse(
                        c.getId(),
                        c.getName(),
                        collectionCardRepository.sumQuantityByCollection_Id(c.getId()),
                        collectionCardRepository.sumTotalPriceByCollection_Id(c.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getById(UUID userId, UUID collectionId) {
        Collection collection = findOwnedCollection(userId, collectionId);
        return toResponse(collection);
    }

    @Override
    @Transactional
    public CollectionResponse create(UUID userId, CollectionRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalStateException("Authenticated user " + userId + " not found")
        );

        Collection collection = new Collection(user, request.name());
        collection = collectionRepository.save(collection);

        List<CollectionCard> items = toCollectionCards(collection, request.items());
        collectionCardRepository.saveAll(items);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public CollectionResponse update(UUID userId, UUID collectionId, CollectionRequest request) {
        Collection collection = findOwnedCollection(userId, collectionId);
        collection.setName(request.name());
        collection = collectionRepository.save(collection);

        collectionCardRepository.deleteAllByCollection_Id(collectionId);
        List<CollectionCard> items = toCollectionCards(collection, request.items());
        collectionCardRepository.saveAll(items);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID collectionId) {
        Collection collection = findOwnedCollection(userId, collectionId);
        collectionCardRepository.deleteAllByCollection_Id(collectionId);
        collectionRepository.delete(collection);
    }

    private Collection findOwnedCollection(UUID userId, UUID collectionId) {
        return collectionRepository.findByIdAndUser_Id(collectionId, userId).orElseThrow(() ->
                new CollectionNotFoundException("Collection with id " + collectionId + " not found")
        );
    }

    private List<CollectionCard> toCollectionCards(Collection collection, List<CollectionItemRequest> items) {
        return items.stream().map(item -> {
            Card card = cardRepository.findById(item.cardId()).orElseThrow(() ->
                    new CardNotFoundException("Card with id " + item.cardId() + " not found")
            );
            CardVariant variant = resolveVariant(item.cardId(), item.variantId());

            CollectionCard cc = new CollectionCard();
            cc.setCollection(collection);
            cc.setCard(card);
            cc.setVariant(variant);
            cc.setQuantity(item.quantity());
            cc.setPrice(item.price());
            return cc;
        }).toList();
    }

    private CardVariant resolveVariant(UUID cardId, UUID variantId) {
        if (variantId == null) return null;
        CardVariant variant = cardVariantRepository.findById(variantId).orElseThrow(() ->
                new VariantNotOwnedByCardException("Variant " + variantId + " not found")
        );
        if (!variant.getCard().getId().equals(cardId)) {
            throw new VariantNotOwnedByCardException("Variant " + variantId + " does not belong to card " + cardId);
        }
        return variant;
    }

    private CollectionResponse toResponse(Collection collection) {
        List<CollectionItemResponse> items = collectionCardRepository.findByCollection_Id(collection.getId())
                .stream()
                .map(CollectionItemResponse::fromCollectionCard)
                .toList();
        return new CollectionResponse(collection.getId(), collection.getName(), items);
    }
}
