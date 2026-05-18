package be.ugent.ledc.chronos.algorithms.changedetection;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.List;

public interface ChangePointDetector<I extends Comparable<? super I>, N extends Number>
{
    public List<I> changes(Signal<I,N> signal) throws ChronosException;
}
