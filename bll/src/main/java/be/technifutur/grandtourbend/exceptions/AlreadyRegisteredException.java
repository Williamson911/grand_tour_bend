package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class AlreadyRegisteredException extends GrandTourBendException {

    public AlreadyRegisteredException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
