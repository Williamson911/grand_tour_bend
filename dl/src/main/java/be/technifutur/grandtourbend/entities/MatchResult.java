package be.technifutur.grandtourbend.entities;

public record MatchResult(
        int round,
        String result,
        String opponentName,
        String opponentLeader
) {}
