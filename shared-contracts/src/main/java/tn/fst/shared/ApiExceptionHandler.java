package tn.fst.shared;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> status(ResponseStatusException e) {
    ProblemDetail p =
        ProblemDetail.forStatusAndDetail(
            e.getStatusCode(), Objects.toString(e.getReason(), "Request failed"));
    return ResponseEntity.status(e.getStatusCode()).body(p);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .distinct()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Invalid input")));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
  }
}
