package be.ugent.ledc.sigma.datastructures.contracts;

public class SigmaContractException extends RuntimeException
{
    public SigmaContractException(String message)
    {
        super(message);
    }

    public SigmaContractException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SigmaContractException(Throwable cause)
    {
        super(cause);
    }

    public SigmaContractException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
