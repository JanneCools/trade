package be.ugent.ledc.chronos;

import be.ugent.ledc.core.LedcException;

public class ChronosException extends LedcException
{
    public ChronosException(String message)
    {
        super(message);
    }

    public ChronosException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ChronosException(Throwable cause)
    {
        super(cause);
    }

    public ChronosException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
