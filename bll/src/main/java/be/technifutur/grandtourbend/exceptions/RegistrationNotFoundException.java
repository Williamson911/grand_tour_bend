package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class RegistrationNotFoundException extends GrandTourBendException {

    public RegistrationNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
