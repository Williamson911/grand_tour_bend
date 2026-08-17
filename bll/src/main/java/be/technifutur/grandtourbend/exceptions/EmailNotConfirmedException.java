package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class EmailNotConfirmedException extends GrandTourBendException {

    public EmailNotConfirmedException() {
        this("Email not confirmed");
    }

    public EmailNotConfirmedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
