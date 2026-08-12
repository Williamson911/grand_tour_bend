package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends GrandTourBendException {

    public InvalidCredentialsException() {
        this("Invalid username or password");
    }

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
