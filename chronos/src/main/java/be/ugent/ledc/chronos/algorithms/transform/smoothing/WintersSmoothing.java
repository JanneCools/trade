package be.ugent.ledc.chronos.algorithms.transform.smoothing;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.core.operators.UnitScore;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * An improvement of Holt exponential smoothing that accounts for a seasonal pattern
 * @author abronsel
 * @param <I> Type of time index
 * @param <N> Type of value
 */
public class WintersSmoothing<I extends Comparable<? super I>, N extends Number> extends HoltSmoothing<I, N>
{
    /**
     * Smoothing factor for the trend
     */
    public double gamma; 

    /**
     * Type of seasonality model
     */
    private final int seasonalityModel;
    
    /**
     * Amount of steps included in one cycleSize of the seasonal pattern
     */
    private final int cycleSize;

    public WintersSmoothing(UnitScore alpha, UnitScore beta, UnitScore gamma, int forecastSteps, int cycleSize, int trendModel, int seasonalityModel) throws ChronosException
    {
        super(alpha, beta, forecastSteps, trendModel);
        this.gamma = gamma.getValue();
        this.seasonalityModel = seasonalityModel;
        this.cycleSize = cycleSize;
    }

    public WintersSmoothing(UnitScore alpha, UnitScore beta, UnitScore gamma, int forecastSteps, int cycleSize) throws ChronosException
    {
        this(alpha, beta, gamma, forecastSteps, cycleSize, ADDITIVE, ADDITIVE);
    }

    @Override
    public Signal<I, Double> smooth(Signal<I, N> original) throws ChronosException
    {
        OrdinalContractor<I> ctr = original.getIndexContractor();
        
        Signal<I, Double> smoothed = new Signal(ctr);
        
        double level = original.valueAt(original.start()).doubleValue();
        double trend = initialTrendEstimate(original);
        double[] cycle = initialCycleEstimate(original);

        for(int i=0; i<original.size() + getForecastSteps(); i++)
        {
            I time = ctr.add(original.start(), i);
            
            //Forecast steps
            if(i >= original.size())
            {
                int k = i - original.size() + 1;
                
                if(getTrendModel() == ADDITIVE && getSeasonalityModel() == ADDITIVE)
                {
                    smoothed.put(
                        time,
                        level + k*trend + cycle[i%cycleSize]
                    );
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == ADDITIVE)
                {
                    smoothed.put(
                        time,
                        level * Math.pow(trend,k) + cycle[i%cycleSize]
                    );
                }
                else if(getTrendModel() == ADDITIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    smoothed.put(
                        time,
                        (level + k*trend) * cycle[i%cycleSize]);
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    smoothed.put(
                        time,
                        level * Math.pow(trend,k) * cycle[i%cycleSize]
                    );
                }
            }
            //Smoothing and update steps
            else
            {
                double prevLevel = level;
                double prevTrend = trend;
                double prevSeas = cycle[i%cycleSize];
                
                double value = original.valueAt(time).doubleValue();
                
                //Update level
                if(getTrendModel() == ADDITIVE && getSeasonalityModel() == ADDITIVE)
                {
                    level = getAlpha() * (value - prevSeas)    
                    + (1.0 - getAlpha()) * (prevLevel + prevTrend);
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == ADDITIVE)
                {
                    level = getAlpha() * (value - prevSeas)    
                    + (1.0 - getAlpha()) * (prevLevel * prevTrend);
                }
                else if(getTrendModel() == ADDITIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    level = getAlpha() * (value / prevSeas)    
                    + (1.0 - getAlpha()) * (prevLevel + prevTrend);
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    level = getAlpha() * (value / prevSeas)    
                    + (1.0 - getAlpha()) * (prevLevel * prevTrend);
                }

                
                //Update trend
                if(getTrendModel() == ADDITIVE)
                {
                   trend = getBeta() * (level - prevLevel) + (1.0 - getBeta()) * prevTrend;
                }
                else if(getTrendModel() == MULTIPLICATIVE)
                {
                   trend = getBeta() * (level / prevLevel) + (1.0 - getBeta()) * prevTrend;
                }
                
                //Update cycle pattern
                if(getSeasonalityModel()== ADDITIVE)
                {
                    cycle[i % cycleSize] = gamma * (value - level) + (1.0 - gamma) * prevSeas;
                }
                else if(getSeasonalityModel() == MULTIPLICATIVE)
                {
                    cycle[i % cycleSize] = gamma * (value / level) + (1.0 - gamma) * prevSeas;
                }
                
                //Update smoothed value
                if(getTrendModel() == ADDITIVE && getSeasonalityModel() == ADDITIVE)
                {
                    smoothed.put(
                        time,
                        level + trend + cycle[i%cycleSize]
                    );
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == ADDITIVE)
                {
                    smoothed.put(
                        time,
                        (level * trend) + cycle[i%cycleSize]
                    );
                }
                else if(getTrendModel() == ADDITIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    smoothed.put(
                        time,
                        (level + trend) * cycle[i%cycleSize]);
                }
                else if(getTrendModel() == MULTIPLICATIVE && getSeasonalityModel() == MULTIPLICATIVE)
                {
                    smoothed.put(
                        time,
                        level * trend * cycle[i%cycleSize]
                    );
                }
            }
        }
        
        
        return smoothed;
    }
    
    private double initialTrendEstimate(Signal<I, N> original) throws ChronosException
    {
        if(cycleSize > original.size() / 2)
            throw new ChronosException("Cycle size "
            + cycleSize
            + " is too big to compute initial trend estimate for signal of size "
            + original.size());
        
        I s = original.start();
        OrdinalContractor<I> ctr = original.getIndexContractor();
        
        //Initial etimate for trend is the average trend over all seasons
        return IntStream
            .range(0, cycleSize)
            .mapToDouble(i -> (
                getTrendModel() == ADDITIVE
                ? original.valueAt(ctr.add(s, cycleSize + i)).doubleValue() - 
                original.valueAt(ctr.add(s, i)).doubleValue()
                :original.valueAt(ctr.add(s, cycleSize + i)).doubleValue() / 
                original.valueAt(ctr.add(s, i)).doubleValue()
                )/cycleSize
            )
            .sum() / cycleSize;
    }
    
    private double[] initialCycleEstimate(Signal<I, N> original) throws ChronosException
    {
        int numberOfCycles = original.size() / cycleSize;
        
        if(numberOfCycles < 1)
            throw new ChronosException("Cycle size "
            + cycleSize
            + " is too big to compute initial cycle estimate for signal of size "
            + original.size());
        
        I s = original.start();
        OrdinalContractor<I> ctr = original.getIndexContractor();
        
        List<Double> levelNormalized = new ArrayList<>();
        
        for(int i=0;i<numberOfCycles; i++)
        {
            final int c = i;
            
            double cycleAvg = IntStream
                .range(0, cycleSize)
                .mapToDouble(j -> original.valueAt(ctr.add(s, c + j)).doubleValue())
                .average()
                .getAsDouble();
            
            IntStream
                .range(0, cycleSize)
                .forEach(j -> levelNormalized.add
                (
                    original
                        .valueAt(ctr.add(s, c + j))
                        .doubleValue()
                    - cycleAvg)
                );
        }
        
        double[] cycleEstimate = new double[cycleSize];
        
        for(int i=0;i<cycleSize; i++)
        {
            final int c = i;
            
            cycleEstimate[i] =  IntStream
                .range(0, numberOfCycles)
                .mapToDouble(j -> levelNormalized.get(c + j))
                .average()
                .getAsDouble();
        }
        
        return cycleEstimate;
    }

    public double getGamma()
    {
        return gamma;
    }

    public int getSeasonalityModel()
    {
        return seasonalityModel;
    }

    public int getCycleSize()
    {
        return cycleSize;
    }
}
