package be.technifutur.grandtourbend.models.eventType.responses;

import be.technifutur.grandtourbend.entities.EventType;

public record EventTypeResponse(
        Long id,
        String name,
        String icon,
        String color
) {

    public static EventTypeResponse fromEventType(EventType eventType) {
        return new EventTypeResponse(
                eventType.getId(),
                eventType.getName(),
                eventType.getIcon(),
                eventType.getColor()
        );
    }
}

