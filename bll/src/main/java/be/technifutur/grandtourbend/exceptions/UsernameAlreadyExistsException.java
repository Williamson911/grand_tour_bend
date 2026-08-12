package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends GrandTourBendException {

    public UsernameAlreadyExistsException() {
        this("Username already exists");
    }

    public UsernameAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
