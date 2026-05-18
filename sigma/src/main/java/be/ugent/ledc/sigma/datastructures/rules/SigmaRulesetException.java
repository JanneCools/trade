package be.ugent.ledc.sigma.datastructures.rules;

public class SigmaRulesetException extends RuntimeException {

    public SigmaRulesetException(String message) {
        super(message);
    }

    public SigmaRulesetException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigmaRulesetException(Throwable cause) {
        super(cause);
    }

    public SigmaRulesetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
