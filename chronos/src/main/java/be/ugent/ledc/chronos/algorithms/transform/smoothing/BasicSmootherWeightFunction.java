package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import java.util.function.Function;

public enum BasicSmootherWeightFunction implements SmootherWeightFunction
{
    UNIFORM(x -> x < 0.0 || x > 1.0 ? 0.0 : 1.0),
    LINEAR(x -> x < 0.0 || x > 1.0 ? 0.0 : 1.0 - x),
    TRICUBE(x -> x < 0.0 || x > 1.0 ? 0.0 : Math.pow(1.0 - Math.pow(x, 3.0), 3.0));
    
    private final Function<Double, Double> latentFunction;

    private BasicSmootherWeightFunction(Function<Double, Double> latentFunction)
    {
        this.latentFunction = latentFunction;
    }

    @Override
    public Double apply(Double t)
    {
        return latentFunction.apply(t);
    }
    
}
