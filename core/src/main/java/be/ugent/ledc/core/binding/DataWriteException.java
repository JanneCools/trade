package be.ugent.ledc.core.binding;

import be.ugent.ledc.core.LedcException;

public class DataWriteException extends LedcException
{
    public DataWriteException(String message)
    {
        super(message);
    }

    public DataWriteException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DataWriteException(Throwable cause)
    {
        super(cause);
    }

    public DataWriteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
