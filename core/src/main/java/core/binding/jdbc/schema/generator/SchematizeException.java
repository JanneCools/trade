package core.binding.jdbc.schema.generator;

import core.binding.BindingException;

public class SchematizeException extends BindingException
{
    public SchematizeException(String string)
    {
        super(string);
    }

    public SchematizeException(Throwable thrwbl)
    {
        super(thrwbl);
    }
}
