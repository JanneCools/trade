package be.ugent.ledc.chronos.algorithms.transform;

import be.ugent.ledc.chronos.algorithms.transform.smoothing.MovingAverageSmoother;
import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;

public class MovingAverageSpikeTransform<I extends Comparable<? super I>, N extends Number> implements Transformator<I, N, I, Double>
{
    private final int smoothingWindow;
    
    private final int differenceWindow;

    public MovingAverageSpikeTransform(int smoothingWindow, int differenceWindow)
    {
        this.smoothingWindow = smoothingWindow;
        this.differenceWindow = differenceWindow;
    }
    
    public MovingAverageSpikeTransform()
    {
        this(10, 3);
    }
    
    @Override
    public Signal<I, Double> transform(Signal<I, N> original) throws ChronosException
    {
        //Find a smoothed signal (moving average smoothing)
        Signal<I, Double> smoothed = new MovingAverageSmoother<I, N>(smoothingWindow).smooth((Signal<I,N>) original);
        
        //Compute differences in smoothed signal with requested offset
        Signal<I, Double> diffSignal = new Signal<>(original.getIndexContractor());
        
        for(I idx: smoothed.indexSet())
        {
            Double value = smoothed.valueAt(idx);
            
            I previousIndex = smoothed.jumpLeft(idx, differenceWindow);
            
            if(previousIndex == null || value == null)
                diffSignal.put(idx, 0.0);
            else
            {
                diffSignal.put(idx, Math.abs(value - smoothed.valueAt(previousIndex)));   
            }
        }
        
        return diffSignal;
    }
    
}
