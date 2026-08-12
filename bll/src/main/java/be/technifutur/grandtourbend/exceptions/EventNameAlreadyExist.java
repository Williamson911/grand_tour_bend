package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class EventNameAlreadyExist extends GrandTourBendException {

    public EventNameAlreadyExist() {
        this("Event already exist");
    }

    public EventNameAlreadyExist(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
