package be.technifutur.grandtourbend;

import be.technifutur.grandtourbend.models.card.responses.CardDetailResponse;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardService {
    Page<CardResponse> getAll(String type, String search, Pageable pageable);
    CardDetailResponse getById(UUID id);
}
