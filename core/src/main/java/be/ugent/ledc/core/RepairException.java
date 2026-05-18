package be.ugent.ledc.core;

public class RepairException extends LedcException
{
    public RepairException(String message)
    {
        super(message);
    }

    public RepairException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RepairException(Throwable cause)
    {
        super(cause);
    }

    public RepairException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
