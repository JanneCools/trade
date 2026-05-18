package be.ugent.ledc.sigma.datastructures.rules;

public class SigmaRuleException extends RuntimeException {

    public SigmaRuleException(String message) {
        super(message);
    }

    public SigmaRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigmaRuleException(Throwable cause) {
        super(cause);
    }

    public SigmaRuleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
