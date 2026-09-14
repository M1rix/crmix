package uz.mirix.crmix.platform.error;

import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.mirix.crmix.platform.web.CorrelationIdFilter;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        return build(exception.status(), exception.code(), exception.title(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        var detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraint(DataIntegrityViolationException exception) {
        return build(HttpStatus.CONFLICT, "data-conflict", "Data conflict", "The operation conflicts with existing data");
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String code, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:crmix:problem:" + code));
        problem.setProperty("code", code);
        problem.setProperty("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return ResponseEntity.status(status).body(problem);
    }
}
