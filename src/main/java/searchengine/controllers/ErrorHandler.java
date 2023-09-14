package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import searchengine.config.ErrorOptionConfig;
import searchengine.services.response.ResponseService;
import searchengine.services.response.ResponseServiceImpl;

@Component
@ControllerAdvice
@RequestMapping("/error-handler")
@RequiredArgsConstructor
public class ErrorHandler {

    private final ErrorOptionConfig errorOptionConfig;

    //Error 400
    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    public ResponseEntity<ResponseService> handleBadRequest() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getBadRequestError()), HttpStatus.BAD_REQUEST);
    }

    //Error 401
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public ResponseEntity<ResponseService> handleUnauthorized() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getUnauthorizedError()), HttpStatus.BAD_REQUEST);
    }

    //Error 403
    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ResponseEntity<ResponseService> handleForbidden() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getForbiddenError()), HttpStatus.BAD_REQUEST);
    }

    //Error 404
    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ResponseEntity<ResponseService> handleNotFound() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getNotFoundError()), HttpStatus.NOT_FOUND);
    }

    //Error 405
    @ExceptionHandler(HttpClientErrorException.MethodNotAllowed.class)
    public ResponseEntity<ResponseService> handleMethodNotAllowed() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getMethodNotAllowedError()), HttpStatus.BAD_REQUEST);
    }

    //Error 500
    @ExceptionHandler(InternalError.class)
    public ResponseEntity<ResponseService> handleInternal() {
        return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getInternalServerError()), HttpStatus.BAD_REQUEST);
    }

}
