package be.ugent.ledc.core.dataset.contractors;

public class ContractorException extends RuntimeException
{
    public ContractorException(String message)
    {
        super(message);
    }

    public ContractorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ContractorException(Throwable cause)
    {
        super(cause);
    }

    public ContractorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
