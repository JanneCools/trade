package be.ugent.ledc.core.cost.errormechanism;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SwapError<T> implements ErrorMechanism<T>
{
    private final Set<String> adjacentAttributes;

    public SwapError(Set<String> adjacentAttributes)
    {
        this.adjacentAttributes = adjacentAttributes;
    }
    
    public SwapError(String... adjacentAttributes)
    {
        this(Stream.of(adjacentAttributes).collect(Collectors.toSet()));
    }
    
    @Override
    public boolean explains(T originalValue, T repairedValue, DataObject originalObject)
    {
        return originalValue != null
            && repairedValue != null
            && adjacentAttributes
            .stream()
            .anyMatch(a -> repairedValue.equals(originalObject.get(a)));
    }

    @Override
    public List<T> explanations(T originalValue, DataObject originalObject)
    {
        return adjacentAttributes
            .stream()
            .filter(a -> originalObject.get(a) != null)
            .map(a -> (T)originalObject.get(a))
            .collect(Collectors.toList());
    }
   
}
