package be.ugent.ledc.chronos.algorithms.transform.cusum;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.transform.Transformator;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.ProbabilityDistribution;
import java.util.List;

public abstract class CusumTransform<I extends Comparable<? super I>, N extends Number, M> implements Transformator<I, N, I, Double>
{
    private final int window;
    
    public CusumTransform(int window)
    {
        this.window = window;
    }
    
    public CusumTransform()
    {
        this(10);
    }

    public int getWindow()
    {
        return window;
    }
    
    @Override
    public Signal<I, Double> transform(Signal<I, N> signal) throws ChronosException
    {
        I idx = signal.jumpRight(signal.start(), getWindow());
        
        Signal<I, Double> cusumTransform = new Signal<>(signal.getIndexContractor());
        
        //Iterate
        for(int i=getWindow(); i<= signal.size() - getWindow(); i++)
        {
            Signal<I, N> left  = signal.subSignal(
                signal.jumpLeft(idx, getWindow()),
                idx);
            
            Signal<I, N> right = signal.subSignal(
                idx,
                signal.jumpRight(idx, getWindow()));
            
            double stat = analyze(left, right);
            cusumTransform.put(idx, stat);
            System.out.println(idx + "\t" + stat);
            idx = signal.nextIndex(idx);
        }
        
        return cusumTransform;
    }
    
    public abstract double analyze(Signal<I, N> left, Signal<I, N> right);
    
    public double cusum(List<M> data, ProbabilityDistribution<M> observed, ProbabilityDistribution<M> alternative)
    {          
        return data
            .stream()
            .mapToDouble
            (n -> - Math.log(alternative.probability(n)/observed.probability(n)))
            .sum();
    }
}
