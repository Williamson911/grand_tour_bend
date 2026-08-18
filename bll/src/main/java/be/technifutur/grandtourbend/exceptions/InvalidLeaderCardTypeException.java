package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidLeaderCardTypeException extends GrandTourBendException {

    public InvalidLeaderCardTypeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
