package be.technifutur.grandtourbend.models.auth.error.responses;

import be.technifutur.grandtourbend.exceptions.GrandTourBendException;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        HttpStatus status,
        Object error
) {

    public static ErrorResponse fromException(GrandTourBendException e) {
        return new ErrorResponse(e.getStatus(),e.getError());
    }
}
