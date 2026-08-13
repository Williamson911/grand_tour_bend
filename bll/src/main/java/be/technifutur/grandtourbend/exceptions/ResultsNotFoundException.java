package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class ResultsNotFoundException extends GrandTourBendException {

    public ResultsNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
