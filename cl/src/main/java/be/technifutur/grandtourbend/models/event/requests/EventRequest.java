package be.technifutur.grandtourbend.models.event.requests;

import be.technifutur.grandtourbend.entities.Address;
import be.technifutur.grandtourbend.entities.Event;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EventRequest(
        @NotBlank String name,
        @NotNull Long eventTypeId,
        @NotNull @Future LocalDate date,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String venue,
        @NotNull Double lat,
        @NotNull Double lng,
        String registerLink
) {

    public Event toEvent() {
        Event event = new Event();
        event.setName(name);
        event.setDate(date);
        event.setAddress(new Address(city, country, venue, lat, lng));
        event.setRegisterLink(registerLink);
        return event;
    }
}
