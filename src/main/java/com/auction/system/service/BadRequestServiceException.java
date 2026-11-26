package com.auction.system.service;

import java.io.Serial;

/**
 * Business rule violation raised by the service layer.
 * <p>
 * Services must not depend on the web layer, so they throw this exception instead of
 * {@code BadRequestAlertException}. The {@code ExceptionTranslator} converts it into the same
 * {@code 400 Bad Request} problem response (title, {@code error.<errorKey>} message key and alert headers).
 */
public class BadRequestServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    public BadRequestServiceException(String defaultMessage, String entityName, String errorKey) {
        super(defaultMessage);
        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
