package be.ugent.ledc.chronos.algorithms.transform;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;

/**
 * An interface that describe a forecasting strategy.The basic operation of a forecaster
 is to predict the value of a signal at k steps into the future.Step size is hereby determined by the contractor of the signal.
 * @author abronsel
 * @param <I> Type of time index
 * @param <D> Type of the value of the signal
 * @param <O> Output type of the forecast
 */
public interface Forecaster<I extends Comparable<? super I>,D, O>
{
    public O forecast(Signal<I,D> signal, int k) throws ChronosException;
}
