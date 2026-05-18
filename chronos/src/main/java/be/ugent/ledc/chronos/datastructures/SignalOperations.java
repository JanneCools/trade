package be.ugent.ledc.chronos.datastructures;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.core.util.SetOperations;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SignalOperations
{
    /**
     * This method returns a pointwise aggregation of two signals. The output signal
     * contains each index that is an index for one of the input signals. 
     * The value for that index is computed by first applying the aggregator function
     * to the values held by the input signals on that index. Next, the convertor function
     * is applied to the output of the aggregator, yielding a value of type O.
     * 
     * Important: if the aggregator is not a symmetric function, then the value
     * of the first signal (s1) is the first argument of the aggregator while the value
     * of the second signal (2) is the second argument of the aggregator.
     * 
     * If the index is missing in one of the input signals, the aggregator is not 
     * applied and the output value is obtained by passing the single input value
     * to the convertor function.
     * 
     * Note: in case one of the values for an index is null, the output value will also be null
     * for that index.
     * @param <I>
     * @param <T>
     * @param <O>
     * @param s1
     * @param s2
     * @param aggregator An aggregator function that combines values of type T
     * @param convertor A convertor that maps values of type T to values of type O
     * @return
     * @throws ChronosException If the input signals do not have the same contract on their index attribute.
     */
    public static <I extends Comparable<? super I>, T, O> Signal<I,O> pointwiseAggregate(Signal<I,T> s1, Signal<I,T> s2, BiFunction<T,T,T> aggregator, Function<T,O> convertor) throws ChronosException
    {
        if(!s1.getIndexContractor().equals(s2.getIndexContractor()))
            throw new ChronosException("Cannot combine signals with different contractors");
        
        Signal<I,O> aggregate = new Signal<>(s1.getIndexContractor());
        
        Set<I> indices = SetOperations.union(s1.indexSet(), s2.indexSet());
        
        for(I idx: indices)
        {
            if(s1.hasValueAt(idx) && s1.valueAt(idx) == null)
                aggregate.put(idx, null);
            else if(s2.hasValueAt(idx) && s2.valueAt(idx) == null)
                aggregate.put(idx, null);
            else if(!s1.hasValueAt(idx))
                aggregate.put(idx, convertor.apply(s2.valueAt(idx)));
            else if(!s2.hasValueAt(idx))
                aggregate.put(idx, convertor.apply(s1.valueAt(idx)));
            else
                aggregate.put(
                    idx,
                    convertor.apply(
                        aggregator.apply(
                            s1.valueAt(idx),
                            s2.valueAt(idx)
                    ))
                );
        }
        
        return aggregate;
    }
    
    /**
     * This method returns a pointwise aggregation of two signals, with no additional conversion.
     * @param <I>
     * @param <T>
     * @param s1
     * @param s2
     * @param aggregator An aggregator function that combines values of type T
     * @return
     * @throws ChronosException If the input signals do not have the same contract on their index attribute.
     */
    public static <I extends Comparable<? super I>, T> Signal<I,T> pointwiseAggregate(Signal<I,T> s1, Signal<I,T> s2, BiFunction<T,T,T> aggregator) throws ChronosException
    {
        return pointwiseAggregate(
            s1,
            s2,
            aggregator,
            Function.identity());
    }
}
