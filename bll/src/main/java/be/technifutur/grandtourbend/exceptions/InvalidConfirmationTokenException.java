package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidConfirmationTokenException extends GrandTourBendException {

    public InvalidConfirmationTokenException() {
        this("Invalid or expired confirmation token");
    }

    public InvalidConfirmationTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
