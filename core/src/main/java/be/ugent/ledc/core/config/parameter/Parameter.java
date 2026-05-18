package be.ugent.ledc.core.config.parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class Parameter<T>
{
    private final String name;
    private final String shortName;
    private final String description;
    private final boolean required;
    private final Function<String, T> creator;
    private final Map<Predicate<T>, String> verifiers;
    
    private final boolean repeating;

    public Parameter(String name, String shortName, boolean required, Function<String, T> creator, String description, boolean repeating)
    {
        this.name = name;
        this.shortName = shortName;
        this.required = required;
        this.creator = creator;
        this.verifiers = new HashMap<>();
        this.description = description;
        this.repeating = repeating;
    }
    
    public Parameter(String name, String shortName, boolean required, Function<String, T> creator, String description)
    {
        this(name, shortName, required, creator, description, false);
    }

    public String getName()
    {
        return name;
    }

    public String getShortName()
    {
        return shortName;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isRequired()
    {
        return required;
    }

    public Function<String, T> getCreator()
    {
        return creator;
    }

    public Map<Predicate<T>, String> getVerifiers()
    {
        return verifiers;
    }

    public boolean isRepeating()
    {
        return repeating;
    }
    
    public Parameter<T> addVerifier(Predicate<T> test, String errorMessage)
    {
        this.verifiers.put(test, errorMessage);
        return this;
    }

}
