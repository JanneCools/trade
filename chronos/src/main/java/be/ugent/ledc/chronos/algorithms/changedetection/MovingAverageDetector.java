package be.ugent.ledc.chronos.algorithms.changedetection;

import be.ugent.ledc.chronos.algorithms.transform.MovingAverageSpikeTransform;
import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageDetector<I extends Comparable<? super I>, N extends Number> implements ChangePointDetector<I, N>
{
    private final MovingAverageSpikeTransform<I, N> transform;
    
    private final double medianMultiplier;

    public MovingAverageDetector(MovingAverageSpikeTransform<I,N> transform, double medianMultiplier)
    {
        this.transform = transform;
        this.medianMultiplier = medianMultiplier;
    }
    
    public MovingAverageDetector()
    {
        this(new MovingAverageSpikeTransform<>(), 3);
    }
    
    @Override
    public List<I> changes(Signal<I,N> signal) throws ChronosException
    {
        System.out.println("Selecting change points based on moving average differences...");
        Signal<I, Double> pSignal = transform.transform(signal);
        
        List<Double> sorted = pSignal
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .map(e -> e.getValue())
            .sorted()
            .collect(Collectors.toList());
        
        if(sorted.size() <= 1)
            return new ArrayList<>();
        
        double median = sorted.get(sorted.size()/2);

        System.out.println("Median difference: " + median);
        System.out.println("Difference threshold: " + median * medianMultiplier);
        
        List<I> changePoints = new ArrayList<>();
        
        for(I i: pSignal.indexSet())
        {
            if(pSignal.previousIndex(i) == null || pSignal.nextIndex(i) == null)
                continue;
            
            Double prev = pSignal.valueAt(pSignal.previousIndex(i));
            Double next = pSignal.valueAt(pSignal.nextIndex(i));
            Double curr = pSignal.valueAt(i);
            
            if(curr > median * medianMultiplier && curr > prev && curr > next)
                changePoints.add(i);
        }
        
        return changePoints;
    }
}
