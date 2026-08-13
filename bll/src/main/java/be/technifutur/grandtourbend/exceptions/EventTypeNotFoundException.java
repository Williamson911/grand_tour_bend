package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class EventTypeNotFoundException extends GrandTourBendException {

    public EventTypeNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
