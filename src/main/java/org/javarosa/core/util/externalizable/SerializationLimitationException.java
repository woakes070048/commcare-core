package org.javarosa.core.util.externalizable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class SerializationLimitationException extends RuntimeException {
    public final int percentOversized;
    public SerializationLimitationException(int percentOversized, Throwable cause, String message) {
        super(message, cause);
        this.percentOversized = percentOversized;
    }
}
