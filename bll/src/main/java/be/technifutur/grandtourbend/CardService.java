package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardFacetsResponse;
import be.technifutur.grandtourbend.models.card.responses.CardPrintingResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CardService {
    Page<CardResponse> getAll(String type, String search, Pageable pageable);
    Page<CardPrintingResponse> getPrintings(String type, String search, String color, String series, String rarity, Pageable pageable);
    CardDetailResponse getById(UUID id);
    CardFacetsResponse getFacets();

    /**
     * Looks up the stored image bytes for a card or card-variant by its
     * {@code imgLink}, checking base cards first and then variants.
     * Empty when nothing matches, or when a match exists but has no stored
     * image yet (e.g. the background backfill hasn't reached it yet).
     */
    Optional<CardImage> getImage(String imgLink);

    record CardImage(byte[] data, String contentType) {}
}
