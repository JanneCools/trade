package be.ugent.ledc.core.operators.aggregation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * General interface for an Aggregator that combines a list of values into a single
 * value.
 *
 * @param <T> The type of values that are aggregated by this Aggregator.
 */
public interface Aggregator<T extends Comparable<? super T>>
{
    /**
     * The main method to aggregate a list of values into a single value.
     * @param values
     * @return 
     */
    T aggregate(List<T> values);
    
    default T aggregate(T ... values)
    {
        return aggregate(Stream.of(values).collect(Collectors.toList()));
    }
}
