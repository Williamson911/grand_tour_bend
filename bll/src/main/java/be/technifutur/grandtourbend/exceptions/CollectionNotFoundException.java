package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class CollectionNotFoundException extends GrandTourBendException {

    public CollectionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
