package be.technifutur.grandtourbend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public abstract class GrandTourBendException extends RuntimeException {

    @Getter
    private final HttpStatus status;

    @Getter
    private final Object error;

    public GrandTourBendException(HttpStatus status, Object o) {
        this.status = status;
        this.error = o;
    }
}
