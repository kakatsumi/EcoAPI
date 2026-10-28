package pe.upeu.ecorecicla;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, Object> detalles;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, Object> detalles) {
        super(message);
        this.status = status;
        this.detalles = detalles;
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getDetalles() { return detalles; }
}
