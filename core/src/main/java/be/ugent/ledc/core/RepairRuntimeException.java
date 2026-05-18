package be.ugent.ledc.core;

public class RepairRuntimeException extends RuntimeException
{
    public RepairRuntimeException(String message)
    {
        super(message);
    }

    public RepairRuntimeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RepairRuntimeException(Throwable cause)
    {
        super(cause);
    }

    public RepairRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
