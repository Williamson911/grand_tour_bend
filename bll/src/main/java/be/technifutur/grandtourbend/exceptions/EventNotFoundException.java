package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class EventNotFoundException extends GrandTourBendException  {

    public EventNotFoundException() {
        this("Ship not found");
    }

    public EventNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
