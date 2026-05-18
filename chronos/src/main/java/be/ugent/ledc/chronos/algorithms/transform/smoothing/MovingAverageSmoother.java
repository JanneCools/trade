package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.ArrayDeque;

/**
 * Computes a smoothed version of a signal by replacing each point by
 * a weighted average of the most recent points in a given time window.
 * 
 * The weights are determined by a weight function that, when not specified,
 * is defaulted to uniform weighting.
 * @author abronsel
 * @param <I>
 * @param <N> 
 */
public class MovingAverageSmoother<I extends Comparable<? super I>, N extends Number> implements Smoother<I,N,Double>
{
    private final int window;
    
    private final SmootherWeightFunction weightFunction;

    public MovingAverageSmoother(int window, SmootherWeightFunction weightFunction) throws ChronosException
    {
        if(window < 0)
            throw new ChronosException("Cannot compute smoothed signal. Cause: window is negative");
        this.window = window;
        this.weightFunction = weightFunction;
    }
    
    public MovingAverageSmoother(int window) throws ChronosException
    {
        this(window, BasicSmootherWeightFunction.UNIFORM);
    }
    
    @Override
    public Signal<I, Double> smooth(Signal<I, N> signal) throws ChronosException
    {
        Signal<I, Double> smoothed = new Signal<>(signal.getIndexContractor());
        
        ArrayDeque<Double> timeStack = new ArrayDeque<>();
        
        for(I t: signal.indexSet())
        {
            timeStack.add(signal.valueAt(t).doubleValue());
            
            if(timeStack.size() > window)
                timeStack.removeFirst();
            
            int i = 0;
            
            double sumOfWeights = 0.0;
            double weightedSum = 0.0;
            
            for(Double d: timeStack)
            {
                double w = weightFunction.apply(((double) i) / ((double) (window - 1)));
                
                sumOfWeights += w;
                weightedSum += w * d;
                
                i++;
            }
            
            smoothed.put(t, weightedSum / sumOfWeights);
                
        }
        
        return smoothed;
    }
}
