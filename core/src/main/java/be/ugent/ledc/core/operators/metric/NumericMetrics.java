package be.ugent.ledc.core.operators.metric;

import be.ugent.ledc.core.DataException;
import java.util.function.BiFunction;

public enum NumericMetrics
{
    MANHATTAN((double[] observation1, double[] observation2) ->
    {
        Double distance = 0.0;

        if(observation1.length != observation2.length)
            throw new DataException("Cannot compute Manhattan distance on arrays of different length.");
        
        for(int i=0;i<observation1.length; i++)
        {
            distance += Math.abs(observation1[i] - observation2[i]);
        }

        return distance;
    }),
    
    EUCLIDEAN((double[] observation1, double[] observation2) ->
    {
        Double distance = 0.0;

        if(observation1.length != observation2.length)
            throw new DataException("Cannot compute Euclidean distance on arrays of different length.");

        for(int i=0;i<observation1.length; i++)
        {
            distance += Math.pow(observation1[i] - observation2[i], 2.0);
        }

        return Math.sqrt(distance);
    });

    private final BiFunction<double[], double[], Double> calculator;

    private NumericMetrics(BiFunction<double[], double[], Double> calculator)
    {
        this.calculator = calculator;
    }

    public Double distance(double[] observation1, double[] observation2)
    {
        if(observation1 == null)
        {
            throw new NullPointerException("First observation is null.");
        }
        
        if(observation2 == null)
        {
            throw new NullPointerException("Second observation is null.");
        }

        return this.calculator.apply(observation1, observation2);
    }

}
