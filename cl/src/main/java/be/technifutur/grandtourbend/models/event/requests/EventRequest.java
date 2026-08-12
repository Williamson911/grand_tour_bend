package be.technifutur.grandtourbend.models.event.requests;

import be.technifutur.grandtourbend.entities.Address;
import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.enums.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EventRequest(
        @NotBlank String name,
        @NotNull EventType type,
        @NotNull @Future LocalDate date,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String venue,
        @NotNull Double lat,
        @NotNull Double lng,
        String registerLink
) {

    public Event toEvent() {
        return new Event(
                name,
                type,
                date,
                new Address(city, country, venue, lat, lng),
                registerLink
        );
    }
}
