package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends GrandTourBendException {

    public EmailAlreadyExistsException() {
        this("Email already exists");
    }

    public EmailAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
