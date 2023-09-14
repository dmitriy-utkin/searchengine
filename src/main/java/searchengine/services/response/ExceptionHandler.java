package searchengine.services.response;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import searchengine.config.ErrorOptionConfig;

@Data
public class ExceptionHandler {

    public ExceptionHandler(Exception exception, ErrorOptionConfig errorOptionConfig) {
        this.exception = exception;
        this.errorOptionConfig = errorOptionConfig;
        setError(exception);
    }

    private final Exception exception;
    private HttpStatus httpStatus;
    private String errorMessage;
    private final ErrorOptionConfig errorOptionConfig;

    private void setError(Exception exception) {
        if (exception.getClass().equals(HttpClientErrorException.Unauthorized.class)) {errorMessage = errorOptionConfig.getUnauthorizedError(); httpStatus = HttpStatus.UNAUTHORIZED;}
        if (exception.getClass().equals(HttpClientErrorException.BadRequest.class)) {errorMessage = errorOptionConfig.getBadRequestError(); httpStatus = HttpStatus.BAD_REQUEST;}
        if (exception.getClass().equals(HttpClientErrorException.Forbidden.class)) {errorMessage = errorOptionConfig.getForbiddenError(); httpStatus = HttpStatus.FORBIDDEN;}
        if (exception.getClass().equals(HttpClientErrorException.NotFound.class)) {errorMessage = errorOptionConfig.getNotFoundError(); httpStatus = HttpStatus.NOT_FOUND;}
        if (exception.getClass().equals(HttpClientErrorException.MethodNotAllowed.class)) {errorMessage = errorOptionConfig.getMethodNotAllowedError(); httpStatus = HttpStatus.METHOD_NOT_ALLOWED;}
        if (exception.getClass().equals(HttpServerErrorException.InternalServerError.class)) {errorMessage = errorOptionConfig.getInternalServerError(); httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;}
    }
}
