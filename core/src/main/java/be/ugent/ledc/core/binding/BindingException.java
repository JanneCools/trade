package be.ugent.ledc.core.binding;

import be.ugent.ledc.core.LedcException;

public class BindingException extends LedcException
{
    public BindingException(String message)
    {
        super(message);
    }

    public BindingException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BindingException(Throwable cause)
    {
        super(cause);
    }

    public BindingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
