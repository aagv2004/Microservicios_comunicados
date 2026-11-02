package com.example.foro.servicio_usuarios.exception;

import java.time.Instant;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> negocio(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.de(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        var campos = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Datos invalidos", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> formato(Exception ex) {
        return ResponseEntity.badRequest().body(ApiError.de(400, "JSON, campos o parametros invalidos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> prohibido(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiError.de(403, "No tienes permiso para esta operacion"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> metodo(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).header("Allow", String.join(", ", ex.getSupportedMethods()))
                .body(ApiError.de(405, "Metodo HTTP no permitido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> contenido(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(415).body(ApiError.de(415, "Usa Content-Type: application/json"));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiError> conflicto(Exception ex) {
        return ResponseEntity.status(409).body(ApiError.de(409,
                "Conflicto de datos: correo ya registrado o recurso modificado. Revisa y reintenta"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inesperado(Exception ex) {
        // No registrar mensajes SQL, cuerpos de peticion ni credenciales.
        log.error("Error inesperado de tipo {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.de(500, "Error interno del servicio"));
    }
}
