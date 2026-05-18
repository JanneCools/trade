package be.ugent.ledc.chronos.algorithms.transform.seasonality.decomposition;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;

public interface Decomposer<I extends Comparable<? super I>, N extends Number>
{
    public Decomposition decompose(Signal<I,N> original, int cycleSize) throws ChronosException;
}
