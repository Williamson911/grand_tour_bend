package be.technifutur.grandtourbend.exceptions;

import org.springframework.http.HttpStatus;

public class ExpenseNotFoundException extends GrandTourBendException {

    public ExpenseNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
