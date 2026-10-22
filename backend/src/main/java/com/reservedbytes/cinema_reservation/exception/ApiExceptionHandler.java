package com.reservedbytes.cinema_reservation.exception;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ProblemDetail> handleReservation(ReservationException exception) {
        return ResponseEntity.status(exception.getStatus())
            .body(ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
 public ResponseEntity<ProblemDetail> integrity(Exception e){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Duplicate name/seat position or referenced record."));}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body."));
    }
}

