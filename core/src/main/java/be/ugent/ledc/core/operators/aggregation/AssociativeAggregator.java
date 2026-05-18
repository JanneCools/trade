package be.ugent.ledc.core.operators.aggregation;

import java.util.List;

/**
 * An extension of aggregators to associative aggregators. Because of associativity,
 * these aggregators can be written in terms of their binary version.
 *
 * @param <T> The type of values that are aggregated by this Associative Aggregator.
 * @author abronsel
 */
public interface AssociativeAggregator<T extends Comparable<T>> extends Aggregator<T>
{
    @Override
    /**
     * The default implementation of the aggregate method now calls the binary
     * version of the aggregation step
     */
    default T aggregate(List<T> values)
    {
        return values.stream().reduce(this::aggregate).orElse(null);
    }

    /**
     * The binary version of the aggregation method, where two values are combined into one. 
     * version of the aggregation step
     * @param a The first value that is aggregated
     * @param b The second value that is aggregated
     * @return The aggregation of values a and b.
     */
    T aggregate(T a, T b);
}
