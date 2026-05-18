package be.ugent.ledc.chronos.algorithms.transform.seasonality.decomposition;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.transform.smoothing.BasicSmootherWeightFunction;
import be.ugent.ledc.chronos.algorithms.transform.smoothing.CenteredSmoothing;
import be.ugent.ledc.chronos.algorithms.transform.smoothing.SmootherWeightFunction;
import be.ugent.ledc.chronos.datastructures.Signal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleDecomposer<I extends Comparable<? super I>, N extends Number> implements Decomposer<I,N>
{
    public static final int ADDITIVE        = 1;
    public static final int MULTIPLICATIVE  = 2;
    
    private final int type;
    
    private final SmootherWeightFunction weightFunction;

    public SimpleDecomposer(SmootherWeightFunction weightFunction, int type)
    {
        this.weightFunction = weightFunction;
        this.type = type;
    }
    
    public SimpleDecomposer()
    {
        this(BasicSmootherWeightFunction.UNIFORM, ADDITIVE);
    }

    public int getType()
    {
        return type;
    }
    
    @Override
    public Decomposition decompose(Signal<I, N> signal, int cycleSize) throws ChronosException
    {        
        Signal<I, Double> seasonalAndNoise = new Signal<>(signal.getIndexContractor());
        Signal<I, Double> seasonal = new Signal<>(signal.getIndexContractor());
        Signal<I, Double> noise = new Signal<>(signal.getIndexContractor());
        
        //Compute a trend
        Signal<I, Double> trend = new CenteredSmoothing<I,N>
        (
            cycleSize,
            weightFunction
        )
        .smooth(signal);
        
        for(I idx: signal.indexSet())
        {
            N value = signal.valueAt(idx);
            
            if(value == null)
            {
                seasonalAndNoise.put(idx, null);
            }
            else if(trend.valueAt(idx) == null)
            {
                seasonalAndNoise.put(idx, value.doubleValue());
            }
            else
            {
                switch (type)
                {
                    case ADDITIVE:
                        seasonalAndNoise.put(
                            idx,
                            value.doubleValue() - trend.valueAt(idx));
                        break;
                    case MULTIPLICATIVE:
                        seasonalAndNoise.put(
                            idx,
                            value.doubleValue() / trend.valueAt(idx));
                        break;
                    default:
                        throw new ChronosException("Could not decompose signal. Cause: unknown decomposition type " + type);
                }
            }
        }
        
        Map<Integer, List<Double>> map = new HashMap<>();
        
        int i=1;
        
        for(I idx: seasonalAndNoise.indexSet())
        {
            map.putIfAbsent(i, new ArrayList<>());
            if(seasonalAndNoise.get(idx) != null)
            {
                map
                    .get(i)
                    .add(seasonalAndNoise.get(idx));
            }
            
            i = (i+1)%cycleSize;
        }
        
        Map<Integer, Double> seasonalCycle = map
        .entrySet()
        .stream()
        .collect(Collectors.toMap
        (
            e -> e.getKey(),
            e -> e.getValue()
                .stream()
                .mapToDouble(d->d)
                .average()
                .getAsDouble()
        ));
        
        i = 0;
        for(I idx: seasonalAndNoise.indexSet())
        {
            seasonal.put(idx, seasonalCycle.get(i));
            
            if(seasonalAndNoise.valueAt(idx) == null)
            {
                noise.put(idx, null);
            }
            else
            {
                switch (type)
                {
                    case ADDITIVE -> noise.put(
                            idx,
                            seasonalAndNoise.valueAt(idx) - seasonal.valueAt(idx)
                        );
                    case MULTIPLICATIVE -> noise.put(
                            idx,
                            seasonalAndNoise.valueAt(idx)/seasonal.valueAt(idx)
                        );
                    default -> throw new ChronosException("Could not decompose signal. Cause: unknown decomposition type " + type);
                }
            }
            
            
            i = (i+1)%cycleSize;
        }
        
        return new Decomposition(trend, seasonal, noise);
    }
}
