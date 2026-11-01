package pe.org.floreseh.finanzas.core.exception;

import java.util.UUID;

public final class ResourceNotFoundException extends DomainException{

    public ResourceNotFoundException(String resourceName, UUID id) {
        String formattedMessage = "El recurso '%s' con identificador [%s] no existe o fue desactivado."
                .formatted(resourceName, id);
        super("RESOURCE_NOT_FOUND", formattedMessage);
    }
}
