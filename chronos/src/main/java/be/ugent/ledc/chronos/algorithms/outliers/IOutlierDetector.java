package be.ugent.ledc.chronos.algorithms.outliers;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.List;

public interface IOutlierDetector<I extends Comparable<? super I>, T>
{
    public List<I> findOutliers(Signal<I,T> signal) throws ChronosException;
}
