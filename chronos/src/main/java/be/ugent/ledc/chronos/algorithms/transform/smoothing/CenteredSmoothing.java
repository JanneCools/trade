package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.ArrayDeque;

public class CenteredSmoothing<I extends Comparable<? super I>, N extends Number> implements Smoother<I, N, Double>
{
    private final int window;
    
    private final SmootherWeightFunction weightFunction;

    public CenteredSmoothing(int window, SmootherWeightFunction weightFunction) throws ChronosException
    {
        if(window < 0)
            throw new ChronosException("Cannot compute smoothed signal. Cause: window is negative");
        this.window = window;
        this.weightFunction = weightFunction;
    }
    
    public CenteredSmoothing(int window) throws ChronosException
    {
        this(window, BasicSmootherWeightFunction.UNIFORM);
    }
    
    @Override
    public Signal<I, Double> smooth(Signal<I, N> signal) throws ChronosException
    {
        Signal<I, Double> smoothed = new Signal<>(signal.getIndexContractor());
        
        ArrayDeque<N> leftStack = new ArrayDeque<>();
        ArrayDeque<N> rightStack = new ArrayDeque<>();
        
        int reach = window/2;
        
        I head = null;
        
        int count = 0;
        
        //Pre fill right stack
        for(I t: signal.indexSet())
        {
            if(count < reach)
            {
                if(!signal.start().equals(t) && signal.valueAt(t) != null)
                {
                    rightStack.add(signal.valueAt(t));
                }
                
                count++;
            }
            else
            {
                head = t;
                break;
            }
        }
        
        for(I t: signal.indexSet())
        {
            if(signal.valueAt(t) != null)
            {
                //Compute smoothed value
                double sumOfWeights = 1.0;
                double weightedSum = signal.valueAt(t).doubleValue();

                int left = 1, right = 1;

                //Account for left stack
                for(N d: leftStack)
                {
                    double w = weightFunction.apply(((double) left) / ((double) reach));

                    sumOfWeights += w;
                    weightedSum += w * d.doubleValue();

                    left++;
                }

                //Account for right stack
                for(N d: rightStack)
                {
                    double w = weightFunction.apply(((double) right) / ((double) reach));

                    sumOfWeights += w;
                    weightedSum += w * d.doubleValue();

                    right++;
                }

                //Add smoothed value
                smoothed.put(t, weightedSum / sumOfWeights);
            }
            else
            {
                smoothed.put(t, null);
            }
            
            //Update left stack
            if(signal.valueAt(t) != null)
                leftStack.addFirst(signal.valueAt(t));
            
            if(leftStack.size() > reach)
                leftStack.removeLast();
            
            //Update right stack
            if(head != null && signal.valueAt(head) != null)
            {
                rightStack.add(signal.valueAt(head));
                head = signal.nextIndex(head);
            }
            
            if(!rightStack.isEmpty())
                rightStack.removeFirst();
                
        }
        
        return smoothed;
    }
}
