package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.BindingException;

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
