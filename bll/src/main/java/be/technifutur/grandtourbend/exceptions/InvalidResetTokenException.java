package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends GrandTourBendException {

    public InvalidResetTokenException() {
        this("Invalid or expired reset token");
    }

    public InvalidResetTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
