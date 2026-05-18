package be.ugent.ledc.sigma.datastructures.atoms;

public class AtomException extends RuntimeException {

    public AtomException(String message) {
        super(message);
    }

    public AtomException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtomException(Throwable cause) {
        super(cause);
    }

    public AtomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
