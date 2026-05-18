package be.ugent.ledc.chronos.algorithms.transform.cusum;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.statistics.ProbabilityDistribution;
import be.ugent.ledc.core.statistics.ProbabilityDistributionFactory;
import be.ugent.ledc.core.statistics.Statistics;
import java.util.stream.Collectors;

public class CusumNormalTransform<I extends Comparable<? super I>, N extends Number> extends CusumTransform<I, N, N>
{
    public CusumNormalTransform(int window)
    {
        super(window);
    }

    public CusumNormalTransform(){}
    
    /**
     *
     * @param left
     * @param right
     * @return
     */
    @Override
    public double analyze(Signal<I, N> left, Signal<I, N> right)
    {
        double lMean = left
            .entrySet()
            .stream()
            .mapToDouble(e -> e.getValue().doubleValue())
            .summaryStatistics()
            .getAverage();
        
        double rMean = right
            .entrySet()
            .stream()
            .mapToDouble(e -> e.getValue().doubleValue())
            .summaryStatistics()
            .getAverage();
                
        double lStdDev = Statistics.stdDev(
            left
            .entrySet()
            .stream()
            .map(e -> e.getValue().doubleValue())
            .collect(Collectors.toList()));

        double rStdDev = Statistics.stdDev(
            right
            .entrySet()
            .stream()
            .map(e -> e.getValue().doubleValue())
            .collect(Collectors.toList()));
        
        ProbabilityDistribution lProb = ProbabilityDistributionFactory
            .createNormalDistribution(lMean, lStdDev);
        ProbabilityDistribution rProb = ProbabilityDistributionFactory
            .createNormalDistribution(rMean, rStdDev);

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
