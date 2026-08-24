package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class VariantNotOwnedByCardException extends GrandTourBendException {

    public VariantNotOwnedByCardException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
