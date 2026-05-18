package be.ugent.ledc.chronos.algorithms.transform.seasonality.decomposition;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.transform.Transformator;
import be.ugent.ledc.chronos.algorithms.transform.smoothing.SmootherWeightFunction;
import be.ugent.ledc.chronos.datastructures.Signal;

public class SeasonalityTransform<I extends Comparable<? super I>, N extends Number> implements Transformator<I, N, I, Double>
{
    public static final int TREND       = 1;
    public static final int SEASONALITY = 2;
    public static final int RESIDUAL    = 4;

    /**
     * The transformation mode is an integer between 1 and 7 that indicates 
     * which parts of the decomposition must be preserved:
     * bit 1: trend
     * bit 2: seasonality
     * bit 3: residual
     */
    private final int transformationMode;
    
    /**
     * Cycle of seasonality to be used in decomposition.
     */
    private final int seasonalityCycle;
    
    private final SimpleDecomposer<I,N> decomposer;
    
    public SeasonalityTransform(SmootherWeightFunction weightFunction, int decompositionType, int transformationMode, int seasonalityCycle) throws ChronosException
    {
        this.transformationMode = transformationMode;
        this.seasonalityCycle = seasonalityCycle;
        this.decomposer = new SimpleDecomposer<>(weightFunction, decompositionType);
        
        if(transformationMode < 0 || transformationMode > 7)
            throw new ChronosException("Transformation mode must be an integer between 0 and 7.");
    }
    
    @Override
    public Signal<I, Double> transform(Signal<I, N> original) throws ChronosException
    {
        Decomposition<I> decomposition = decomposer.decompose(
            original,
            seasonalityCycle);
        
        Signal<I, Double> reconstruction = new Signal<>(original.getIndexContractor());
        
        for(I idx: original.indexSet())
        {
            switch(decomposer.getType())
            {
                case SimpleDecomposer.ADDITIVE:
                    reconstruction.put(
                        idx,
                        additiveReconstruction(
                            decomposition.getTrend().get(idx),
                            decomposition.getSeasonal().get(idx),
                            decomposition.getNoise().get(idx)
                        )
                    );
                    break;
                case SimpleDecomposer.MULTIPLICATIVE:
                    reconstruction.put(
                        idx,
                        multiplicativeReconstruction(
                            decomposition.getTrend().get(idx),
                            decomposition.getSeasonal().get(idx),
                            decomposition.getNoise().get(idx)
                        )
                    );
                    break;
                default:
                    throw new ChronosException("Unknown decomposition type. Value " + decomposer.getType());
            }
        }
        
        return reconstruction;
    }
    
    private double additiveReconstruction(double trend, double seasonality, double residual)
    {
        double r = 0.0;
        
        r += (transformationMode & TREND) == TREND ? trend : 0.0;
        r += (transformationMode & SEASONALITY) == SEASONALITY ? seasonality : 0.0;
        r += (transformationMode & RESIDUAL) == RESIDUAL ? residual : 0.0;
        
        return r;
    }
    
    private double multiplicativeReconstruction(double trend, double seasonality, double residual)
    {
        if(transformationMode == 0)
            return 0.0;
        
        double r = 1.0;
        
        r *= (transformationMode & TREND) == TREND ? trend : 1.0;
        r *= (transformationMode & SEASONALITY) == SEASONALITY ? seasonality : 1.0;
        r *= (transformationMode & RESIDUAL) == RESIDUAL ? residual : 1.0;
        
        return r;
    }
}
