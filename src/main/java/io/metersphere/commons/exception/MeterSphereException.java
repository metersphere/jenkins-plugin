package io.metersphere.commons.exception;

public class MeterSphereException extends RuntimeException {
    private static final long serialVersionUID = -649559784594858788L;

    public MeterSphereException() {
    }

    public MeterSphereException(String message, Throwable cause) {
        super(message, cause);
    }

    public MeterSphereException(String message) {
        super(message);
    }

    public MeterSphereException(Throwable cause) {
        super(cause);
    }
}
