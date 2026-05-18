package be.ugent.ledc.sigma.datastructures.formulas;

public class CPFException extends RuntimeException {

    public CPFException(String message) {
        super(message);
    }

    public CPFException(String message, Throwable cause) {
        super(message, cause);
    }

    public CPFException(Throwable cause) {
        super(cause);
    }

    public CPFException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
