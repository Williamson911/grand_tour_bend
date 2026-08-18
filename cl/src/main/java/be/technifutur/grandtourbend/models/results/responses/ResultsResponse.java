package be.technifutur.grandtourbend.models.results.responses;

import be.technifutur.grandtourbend.entities.MatchResult;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.models.card.responses.CardResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ResultsResponse(
        UUID id,
        UUID userId,
        UUID eventId,
        String deckName,
        CardResponse leaderCard,
        Integer placement,
        Integer totalPlayers,
        BigDecimal prizes,
        String notes,
        List<MatchResult> matches,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResultsResponse fromResults(Results r) {
        return new ResultsResponse(
                r.getId(),
                r.getUser().getId(),
                r.getEvent().getId(),
                r.getDeckName(),
                CardResponse.fromCard(r.getLeaderCard()),
                r.getPlacement(),
                r.getTotalPlayers(),
                r.getPrizes(),
                r.getNotes(),
                r.getMatches(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
