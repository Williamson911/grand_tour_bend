package be.technifutur.grandtourbend.models.results.requests;

import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.entities.MatchResult;
import be.technifutur.grandtourbend.entities.Results;
import be.technifutur.grandtourbend.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ResultsRequest(
        @NotBlank String deckName,
        @NotBlank String leaderPlayed,
        @NotNull Integer placement,
        @NotNull Integer totalPlayers,
        BigDecimal prizes,
        String notes,
        List<MatchResult> matches
) {
    public Results toResults (User user, Event event) {
        return new Results(user, event, deckName, leaderPlayed, placement, totalPlayers, prizes, notes, matches);
    }

    public void applyTo(Results results) {
        results.setDeckName(deckName);
        results.setLeaderPlayed(leaderPlayed);
        results.setPlacement(placement);
        results.setTotalPlayers(totalPlayers);
        results.setPrizes(prizes);
        results.setNotes(notes);
        results.setMatches(matches);
    }

}
