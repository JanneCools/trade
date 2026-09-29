package core.config.parameter;

import core.LedcException;

public class ParameterException extends LedcException
{
    public ParameterException(String message)
    {
        super(message);
    }

    public ParameterException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ParameterException(Throwable cause)
    {
        super(cause);
    }
    
}
