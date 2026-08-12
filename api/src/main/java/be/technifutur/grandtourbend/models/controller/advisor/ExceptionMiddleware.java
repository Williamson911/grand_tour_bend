package be.technifutur.grandtourbend.models.controller.advisor;

import be.technifutur.grandtourbend.exceptions.GrandTourBendException;
import be.technifutur.grandtourbend.models.auth.error.responses.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionMiddleware {

    @ExceptionHandler({GrandTourBendException.class})
    public ResponseEntity<?> handleCustom(GrandTourBendException e) {

        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.fromException(e));
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(HttpStatus.FORBIDDEN, "Access is denied"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<?> handleInvalid(MethodArgumentNotValidException e) {

        var errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (m1, m2) -> m1
                ));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<?> handleRuntime(RuntimeException e) {

        return ResponseEntity.status(500).body(e);
    }
}
