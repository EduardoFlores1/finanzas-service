package pe.org.floreseh.finanzas.core.exception;

public abstract class DomainException extends RuntimeException{

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        if(errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("El código de error de dominio es requerido");
        }

        String normalizeCode = errorCode.strip().toLowerCase();

        super(message);
        this.errorCode = normalizeCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
