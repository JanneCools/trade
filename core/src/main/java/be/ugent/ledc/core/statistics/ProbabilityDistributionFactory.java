package be.ugent.ledc.core.statistics;

import be.ugent.ledc.core.DataException;
import java.util.stream.LongStream;

public class ProbabilityDistributionFactory
{
    public static <N extends Number> ProbabilityDistribution<N> createNormalDistribution(double m, double std)
    {
        if(std < 0)
            throw new DataException("Cannot create normal distribution. Cause: standard deviation is smaller than or equal to zero.");
        return (N x) -> Math.exp(-0.5 * Math.pow((x.doubleValue() - m)/std, 2)) / (std * Math.sqrt(2.0 * Math.PI));
    }
    
    public static ProbabilityDistribution<Integer> createPoissonDistribution(double lambda)
    {
        return (Integer k) ->  Math.pow(lambda, k) * Math.exp(-lambda) / (double)(LongStream
            .rangeClosed(1, k)
            .reduce(1, (long x, long y) -> x * y));
    }
    
    public static <N extends Number> ProbabilityDistribution<N> createExponentialDistribution(double lambda)
    {
        return (N x) -> x.doubleValue() < 0 ? 0.0 : lambda * Math.exp(-lambda * x.doubleValue());
    }
    
    public static <N extends Number> ProbabilityDistribution<N> createUniformDistribution(double a, double b)
    {
        if(a >= b)
            throw new DataException("Cannot create uniform distribution. Cause: lower bound is greater than upper bound");
        
        return (N x) -> (x.doubleValue() >= a && x.doubleValue() <= b ? 1.0 / (b-a) : 0.0);
    }
}
