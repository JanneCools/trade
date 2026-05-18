package be.ugent.ledc.chronos.algorithms.transform;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;

/**
 * A transformator takes an original signal and transforms it into a new signal
 * @author abronsel
 * @param <I> Type of index of the original signal
 * @param <T> Type of values of the original signal
 * @param <P> Type of index of the transformed signal
 * @param <S> Type of values for the transformed signal
 */
public interface Transformator<I extends Comparable<? super I>, T, P extends Comparable<? super P>, S>
{
    public Signal<P,S> transform(Signal<I,T> original)  throws ChronosException;
}
