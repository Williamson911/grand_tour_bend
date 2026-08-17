package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends GrandTourBendException {

    public UserNotFoundException() {
        this("User not found");
    }

    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
