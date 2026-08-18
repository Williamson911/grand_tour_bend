package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class CardNotFoundException extends GrandTourBendException {

    public CardNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
