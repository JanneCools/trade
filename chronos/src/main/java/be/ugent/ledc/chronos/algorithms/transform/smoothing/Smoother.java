package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.transform.Transformator;
import be.ugent.ledc.chronos.datastructures.Signal;

public interface Smoother<I extends Comparable<? super I>, T, S> extends Transformator<I,T,I,S>
{
    public Signal<I, S> smooth(Signal<I, T> original) throws ChronosException;
    
    @Override
    public default Signal<I, S> transform(Signal<I, T> original) throws ChronosException
    {
        return smooth(original);
    }
}
