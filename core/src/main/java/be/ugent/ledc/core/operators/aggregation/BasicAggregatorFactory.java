package be.ugent.ledc.core.operators.aggregation;

import be.ugent.ledc.core.operators.UnitScore;

/**
 * A Factory class for some common Aggregation operators on real values
 * @author abronsel
 */
public class BasicAggregatorFactory
{
    /**
     * Return an aggregator that aggregates double values to their arithmetic mean.
     * @return 
     */
    public static Aggregator<Double> createArithmeticMeanDoubleAggregator()
    {
        return (values) -> values
            .stream()
            .mapToDouble(d->d)
            .average()
            .getAsDouble();
    }
    
    /**
     * Return an aggregator that aggregates double values to their harmonic mean.
     * @return 
     */
    public static Aggregator<Double> createHarmonicMeanDoubleAggregator()
    {
        return (values) ->
            ((double)values.size()) /
            values
                .stream()
                .mapToDouble(d->1/d)
                .sum();
    }

    /**
     * Return an aggregator that aggregates double values to their geometric mean.
     * @return 
     */
    public static Aggregator<Double> createGeometricMeanDoubleAggregator()
    {
        return (values) -> Math.pow(values.stream().mapToDouble(d->d).reduce(1, (a,b)->a*b), 1.0 / ((double)values.size()));
    }
    
    /**
     * Return an aggregator that aggregates double values to their sum.
     * @return 
     */
    public static Aggregator<Double> createSumDoubleAggregator()
    {
        return (values) -> values.stream().mapToDouble(d->d).sum();
    }
    
    /**
     * Return an aggregator that aggregates double values to their median.
     * @return 
     */
    public static Aggregator<Double> createMedianDoubleAggregator()
    {
        return (values) -> values.size() % 2 == 0
            ? values.stream().sorted().mapToDouble(d->d).skip(values.size()/2-1).limit(2).average().getAsDouble()        
            : values.stream().sorted().mapToDouble(d->d).skip(values.size()/2).findFirst().getAsDouble();
    }
    
    /**
     * Return an aggregator that aggregates UnitScores values to their mean.
     * @return 
     */
    public static Aggregator<UnitScore> createMeanUnitAggregator()
    {
        return (values) -> new UnitScore(values.stream().mapToDouble(u->u.getValue()).average().getAsDouble());
    }

    /**
     * Return an aggregator that aggregates double values to their median.
     * @return 
     */
    public static Aggregator<UnitScore> createMedianUnitAggregator()
    {
        return (values) -> values.size() % 2 == 0
            ? new UnitScore(values.stream().sorted().mapToDouble(u->u.getValue()).skip(values.size()/2-1).limit(2).average().getAsDouble())        
            : new UnitScore(values.stream().sorted().mapToDouble(u->u.getValue()).skip(values.size()/2).findFirst().getAsDouble());
    }
}
