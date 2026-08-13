package be.technifutur.grandtourbend.models.event.reponses;

import be.technifutur.grandtourbend.entities.Event;
import be.technifutur.grandtourbend.models.address.responses.AddressResponse;
import be.technifutur.grandtourbend.models.eventType.responses.EventTypeResponse;

import java.time.LocalDate;
import java.util.UUID;

public record EventIndexResponse(
        UUID id,
        String name,
        EventTypeResponse type,
        LocalDate date,
        AddressResponse address,
        String registerLink,
        boolean registered
) {

    public static EventIndexResponse fromEvent(Event e, boolean registered) {
        return new EventIndexResponse(
                e.getId(),
                e.getName(),
                EventTypeResponse.fromEventType(e.getType()),
                e.getDate(),
                AddressResponse.fromAddress(e.getAddress()),
                e.getRegisterLink(),
                registered
        );
    }
}
