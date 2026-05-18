package be.ugent.ledc.core;

public class LedcException extends Exception
{
    public LedcException(){}

    public LedcException(String message)
    {
        super(message);
    }

    public LedcException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public LedcException(Throwable cause)
    {
        super(cause);
    }

    public LedcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
