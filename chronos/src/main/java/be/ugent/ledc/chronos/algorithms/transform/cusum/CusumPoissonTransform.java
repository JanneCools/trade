package be.ugent.ledc.chronos.algorithms.transform.cusum;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.ProbabilityDistribution;
import be.ugent.ledc.core.statistics.ProbabilityDistributionFactory;
import java.util.stream.Collectors;

/**
 * A transform that produces the CUSUM statistic in case data are generated
 * by a Poisson distribution. In case the estimated mean within a window is
 * zero, the mean will be adjusted to 1/window to avoid infinite peaks in the
 * CUSUM sequence.
 * 
 * @author abronsel
 * @param <I> 
 */
public class CusumPoissonTransform<I extends Comparable<? super I>> extends CusumTransform<I, Integer, Integer>
{

    public CusumPoissonTransform(int window)
    {
        super(window);
    }

    public CusumPoissonTransform(){}
    
    @Override
    public double analyze(Signal<I, Integer> left, Signal<I, Integer> right)
    {   
        
        double lMean = left
            .entrySet()
            .stream()
            .mapToInt(e -> e.getValue())
            .summaryStatistics()
            .getAverage();
        
        double rMean = right
            .entrySet()
            .stream()
            .mapToInt(e -> e.getValue())
            .summaryStatistics()
            .getAverage();
                
        ProbabilityDistribution lProb = ProbabilityDistributionFactory
            .createPoissonDistribution(lMean == 0 ? 1.0/getWindow() : lMean);
        ProbabilityDistribution rProb = ProbabilityDistributionFactory
            .createPoissonDistribution(rMean == 0 ? 1.0/getWindow() : rMean);

        double d1 = cusum(
            left
                .entrySet()
                .stream()
                .map(e -> e.getValue())
                .collect(Collectors.toList()),
            lProb,
            rProb); 
        
        double d2 = cusum(
            right
                .entrySet()
                .stream()
                .map(e -> e.getValue())
                .collect(Collectors.toList()),
            rProb,
            lProb); 
        
        return Math.max(d1,d2);
    }
    
}
