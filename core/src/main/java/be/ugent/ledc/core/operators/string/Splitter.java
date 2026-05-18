package be.ugent.ledc.core.operators.string;

import be.ugent.ledc.core.datastructures.Multiset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface Splitter extends Function<String, List<String>>
{
    public default Multiset<String> splitToBag(String s)
    {
        return new Multiset<>(apply(s));
    }
    
    public default List<String> split(String s)
    {
        return s == null
            ? new ArrayList<>()
            : apply(s);
    }
}
