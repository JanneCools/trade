package be.ugent.ledc.core.datastructures;

import be.ugent.ledc.core.LedcException;

public class DependencyGraphException extends LedcException
{
    public DependencyGraphException(){}

    public DependencyGraphException(String message)
    {
        super(message);
    }

    public DependencyGraphException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DependencyGraphException(Throwable cause)
    {
        super(cause);
    }

    public DependencyGraphException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
