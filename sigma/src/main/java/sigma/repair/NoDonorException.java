package sigma.repair;

import core.RepairException;

public class NoDonorException extends RepairException
{

    public NoDonorException(String message)
    {
        super(message);
    }

    public NoDonorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoDonorException(Throwable cause)
    {
        super(cause);
    }

    public NoDonorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
