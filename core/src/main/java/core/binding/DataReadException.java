package core.binding;

import core.LedcException;

public class DataReadException extends LedcException
{
    public DataReadException(String message)
    {
        super(message);
    }

    public DataReadException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DataReadException(Throwable cause)
    {
        super(cause);
    }

    public DataReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
