package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

/**
 * An improvement of simple exponential smoothing that accounts for a long-term trend
 * in the data.
 * @author abronsel
 * @param <I>
 * @param <N>
 */
public class HoltSmoothing<I extends Comparable<? super I>, N extends Number> extends SimpleExponentialSmoothing<I, N>
{
    /**
     * Fixed integer indicating an additive compositional model
     */
    public static final int ADDITIVE = 0;
    
    /**
     * Fixed integer indicating a multiplicative compositional model
     */
    public static final int MULTIPLICATIVE = 1;
    
    /**
     * Type of trend model
     */
    private final int trendModel;
    
    /**
     * Smoothing factor for the trend estimation
     */
    private final double beta;
    
    public HoltSmoothing(UnitScore alpha, UnitScore beta, int forecastSteps, int trendModel) throws ChronosException
    {
        super(alpha, forecastSteps);
        this.trendModel = trendModel;
        this.beta = beta.getValue();
        
        if(trendModel != ADDITIVE && trendModel!= MULTIPLICATIVE)
            throw new ChronosException("Unknown trend model type " + trendModel);
    }
    
    public HoltSmoothing(UnitScore alpha, UnitScore beta, int forecastSteps) throws ChronosException
    {
        this(alpha, beta, forecastSteps, ADDITIVE);
    }

    @Override
    public Signal<I, Double> smooth(Signal<I, N> original) throws ChronosException
    {
        OrdinalContractor<I> ctr = original.getIndexContractor();
        
        //Get the level estimates, but truncate the level forecasts
        Signal<I,Double> smoothed = super.smooth(original);
        
        if(getForecastSteps() == 0)
            return smoothed;

        double trend = 0.0;
        
        //Initial estimate for trend 
        if(getTrendModel() == ADDITIVE)
            trend =
                original.valueAfter(original.start()).doubleValue() -
                original.valueAt(original.start()).doubleValue();
        else if(getTrendModel() == MULTIPLICATIVE) 
            trend =
            original.valueAfter(original.start()).doubleValue() /
            original.valueAt(original.start()).doubleValue();
        

        //Prepare to iterate over the signal indices
        I index = original.nextIndex(original.start());
        
        while(index != null)
        {   
            //Update trend
            if(getTrendModel() == ADDITIVE)
                trend = 
                    beta * (smoothed.valueAt(index) - smoothed.valueBefore(index)) +
                    (1.0 - beta) * trend;
            else if(getTrendModel() == MULTIPLICATIVE)            
                trend = 
                    beta * (smoothed.valueAt(index) / smoothed.valueBefore(index)) +
                    (1.0 - beta) * trend;
            index = original.nextIndex(index);
        }
        
        //Add forecast steps
        for(int k=1; k <= getForecastSteps(); k++)
        {
            //Initialize forecast at last known level
            double forecast = smoothed.valueAt(original.end());
            
            switch (trendModel)
            {
                case ADDITIVE:
                    forecast += k * trend;
                    break;
                case MULTIPLICATIVE:
                    forecast *= Math.pow(trend, k);
                    break;
                default:
                    //We leave the forecast as is
            }
            
            smoothed.put(
                ctr.add(original.end(), k),
                forecast
            );
        }
        
        return smoothed;
    }

    public int getTrendModel()
    {
        return trendModel;
    }

    public double getBeta()
    {
        return beta;
    }    
}
