package be.ugent.ledc.core.config.parameter;

import java.util.function.Function;

public class OptionalParameter<T> extends Parameter<T>
{
    private final T defaultValue;
    
    public OptionalParameter(String name, String shortName, Function<String, T> creator, String description, T defaultValue, boolean repeating)
    {
        super(name, shortName, false, creator, description, repeating);
        this.defaultValue = defaultValue;
    }
    
    public OptionalParameter(String name, String shortName, Function<String, T> creator, String description, T defaultValue)
    {
        this(name, shortName, creator, description, defaultValue, false);
    }

    public T getDefaultValue()
    {
        return defaultValue;
    }    
}
