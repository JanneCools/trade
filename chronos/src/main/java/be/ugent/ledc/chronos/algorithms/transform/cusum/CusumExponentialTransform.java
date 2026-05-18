package be.ugent.ledc.chronos.algorithms.transform.cusum;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.ProbabilityDistribution;
import be.ugent.ledc.core.statistics.ProbabilityDistributionFactory;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.stream.Collectors;

/**
 * This transform measures the likelihood that a difference in the gaps between
 * measures is observed.
 * @author abronsel
 * @param <I>
 * @param <N> 
 */
public class CusumExponentialTransform<I extends Comparable<? super I>, N extends Number> extends CusumTransform<I, N, Long>
{
    public CusumExponentialTransform(int window)
    {
        super(window);
    }

    public CusumExponentialTransform(){}
    
    @Override
    public double analyze(Signal<I, N> left, Signal<I, N> right)
    {
        double meanGapLeft = left.interMeasureGap()
            .stream()
            .mapToLong(l -> l)
            .summaryStatistics()
            .getAverage();
        
        double meanGapRight = right.interMeasureGap()
            .stream()
            .mapToLong(l -> l)
            .summaryStatistics()
            .getAverage();
        
        ProbabilityDistribution<Long> lProb = ProbabilityDistributionFactory
            .createExponentialDistribution(meanGapLeft);
        ProbabilityDistribution<Long> rProb = ProbabilityDistributionFactory
            .createExponentialDistribution(meanGapRight);

        double d1 = cusum(
            left.interMeasureGap()
                .stream()
                .collect(Collectors.toList()),
            lProb,
            rProb); 
        
        double d2 = cusum(
            right.interMeasureGap()
                .stream()
                .collect(Collectors.toList()),
            rProb,
            lProb); 

        return Math.max(d1,d2);
    }
}
