package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;

    @Override
    public Page<CardResponse> getAll(String type, String search, Pageable pageable) {
        boolean hasType = type != null && !type.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        Page<Card> cards;
        if (hasType && hasSearch) {
            cards = cardRepository.findByCardTypeAndNameContainingIgnoreCase(type, search, pageable);
        } else if (hasType) {
            cards = cardRepository.findByCardType(type, pageable);
        } else if (hasSearch) {
            cards = cardRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            cards = cardRepository.findAll(pageable);
        }

        return cards.map(CardResponse::fromCard);
    }

    @Override
    public CardDetailResponse getById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + " not found"));

        return CardDetailResponse.fromCard(card);
    }
}
