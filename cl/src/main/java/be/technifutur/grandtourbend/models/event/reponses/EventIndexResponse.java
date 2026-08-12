package be.technifutur.grandtourbend.models.event.reponses;

import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.enums.EventType;
import be.technifutur.grandtourbend.models.address.responses.AddressResponse;

import java.time.LocalDate;

public record EventIndexResponse(
        String id,
        String name,
        EventType type,
        LocalDate date,
        AddressResponse address,
        String registerLink
) {

    public static EventIndexResponse fromEvent(Event e) {
        return new EventIndexResponse(
                e.getId(),
                e.getName(),
                e.getType(),
                e.getDate(),
                AddressResponse.fromAddress(e.getAddress()),
                e.getRegisterLink()
        );
    }
}
