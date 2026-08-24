package be.technifutur.grandtourbend.services.impls;

import be.technifutur.grandtourbend.CardService;
import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.exceptions.CardNotFoundException;
import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import be.technifutur.grandtourbend.repositories.CardPrintingProjection;
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
            cards = cardRepository.searchByCardTypeAndNameOrBackName(type, search, pageable);
        } else if (hasType) {
            cards = cardRepository.findByCardType(type, pageable);
        } else if (hasSearch) {
            cards = cardRepository.searchByNameOrBackName(search, pageable);
        } else {
            cards = cardRepository.findAll(pageable);
        }

        return cards.map(CardResponse::fromCard);
    }

    @Override
    public Page<CardPrintingResponse> getPrintings(String type, String search, String color, String series, Pageable pageable) {
        String normalizedType = blankToNull(type);
        String normalizedSearch = blankToNull(search);
        String normalizedColor = blankToNull(color);
        String normalizedSeries = blankToNull(series);
        return cardRepository
                .findPrintings(normalizedType, normalizedSearch, normalizedColor, normalizedSeries, pageable)
                .map(this::toPrintingResponse);
    }

    private CardPrintingResponse toPrintingResponse(CardPrintingProjection p) {
        return new CardPrintingResponse(
                p.getCardId(),
                p.getVariantId(),
                p.getName(),
                p.getBackName(),
                p.getCardType(),
                p.getColor(),
                p.getCardNumber(),
                p.getSeries(),
                p.getRarity(),
                p.getImgLink()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public CardDetailResponse getById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + " not found"));

        return CardDetailResponse.fromCard(card);
    }
}
