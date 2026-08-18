package be.technifutur.grandtourbend.models.results.requests;

import be.technifutur.grandtourbend.entities.Card;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.MatchResult;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResultsRequest(
        @NotBlank String deckName,
        @NotNull UUID leaderCardId,
        @NotNull Integer placement,
        @NotNull Integer totalPlayers,
        BigDecimal prizes,
        String notes,
        List<MatchResult> matches
) {
    public Results toResults(User user, Event event, Card leaderCard) {
        return new Results(user, event, deckName, leaderCard, placement, totalPlayers, prizes, notes, matches);
    }

    public void applyTo(Results results, Card leaderCard) {
        results.setDeckName(deckName);
        results.setLeaderCard(leaderCard);
        results.setPlacement(placement);
        results.setTotalPlayers(totalPlayers);
        results.setPrizes(prizes);
        results.setNotes(notes);
        results.setMatches(matches);
    }
}
