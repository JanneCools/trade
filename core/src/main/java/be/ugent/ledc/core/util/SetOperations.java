package be.ugent.ledc.core.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetOperations
{
    public static <T> Set<T> set(T... s)
    {
        return Stream.of(s).collect(Collectors.toSet());
    }
    
    public static <T> Set<T> intersect(Set<T> a, Set<T> b)
    {
        Set<T> intersect = a.stream()
                .filter(b::contains)
                .collect(Collectors.toSet());
        
        return intersect;
    }
    
    public static <T> Set<T> union(Set<T> a, Set<T> b)
    {
        return Stream.concat(
            a.stream(),
            b.stream())
        .collect(Collectors.toSet()); 
    }
    
    public static <T> Set<T> difference(Set<T> a, Set<T> b)
    {
        return a.stream()
            .filter(v -> !b.contains(v))
            .collect(Collectors.toSet());
    }
}
