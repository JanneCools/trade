package be.ugent.ledc.core.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListOperations
{
    public static <T> List<T> list(T... s)
    {
        return Stream.of(s).collect(Collectors.toList());
    }
    
    public static <T> Set<T> asSet(List<T> list)
    {
        return list.stream().collect(Collectors.toSet());
    }
}
