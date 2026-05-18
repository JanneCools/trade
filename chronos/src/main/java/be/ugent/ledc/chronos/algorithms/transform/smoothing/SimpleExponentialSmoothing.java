package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

/**
 * Implements simple exponential smoothing with a single level-update equation.
 * It assumes the signal is composed solely of a level that requires estimation.
 * 
 * Exponential smoothing allows for an extension of the signal of a certain steps
 * after the last observations. This can be thought of as a forecast of future observations.
 * @author abronsel
 * @param <I> Type of time index
 * @param <N> Type of value (must be numeric) the original signal has.
 */
public class SimpleExponentialSmoothing<I extends Comparable<? super I>, N extends Number> implements Smoother<I, N, Double>
{
    /**
     * The smoothing parameter for level smoothing.
     */
    private final double alpha;
    
    /**
     * The number of forecast steps to be taken
     */
    private final int forecastSteps;

    public SimpleExponentialSmoothing(UnitScore alpha, int forecastSteps)
    {
        this.alpha = alpha.getValue();
        this.forecastSteps = Math.max(0, forecastSteps);
    }

    @Override
    public Signal<I, Double> smooth(Signal<I, N> original) throws ChronosException
    {
        //Contractor of the original signal
        OrdinalContractor<I> ctr = original.getIndexContractor();
        
        //Create smoothed signal
        Signal<I,Double> smoothed = new Signal<>(ctr);
        
        //Initialize: the smoothed signal start is the same as the original
        smoothed.put(
            original.start(),
            original.valueAt(original.start()).doubleValue()
        );
        
        //Prepare to iterate over the signal indices
        I index = original.nextIndex(original.start());
        
        while(index != null)
        {
            double v = original
                .valueAt(index)
                .doubleValue();
            
            smoothed.put(
                index,
                alpha * v + (1.0 - alpha) * smoothed.get(smoothed.end())
            );
            
            index = original.nextIndex(index);
        }
        
        //Add forecast steps
        for(int k=1; k <= forecastSteps; k++)
        {
            smoothed.put(
                ctr.add(original.end(), k),      //Forecast step k
                smoothed.valueAt(original.end()) //Forecast is last known level
            );
        }

        return smoothed;
    }

    public double getAlpha()
    {
        return alpha;
    }

    public int getForecastSteps()
    {
        return forecastSteps;
    }
}
