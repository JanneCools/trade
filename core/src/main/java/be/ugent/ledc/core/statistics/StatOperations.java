package be.ugent.ledc.core.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StatOperations
{
    public static <I> Map<I, Double> conflate(Map<I, Double> d1, Map<I, Double> d2)
    {
        Set<I> keys = new HashSet<>(d1.keySet());
        keys.addAll(d2.keySet());
        
        //Compute normalization factor
        double norm = keys
            .stream()
            .mapToDouble(idx ->
                    (d1.get(idx) == null ? 0.0 : d1.get(idx))
                *   (d2.get(idx) == null ? 0.0 : d2.get(idx))
            )
            .sum();

        Map<I,Double> conflation = new HashMap<>();
        
        for(I idx: keys)
        {
            if(d1.get(idx) == null || d2.get(idx) == null)
                conflation.put(idx, 0.0);
            else
                conflation.put(idx, (d1.get(idx) * d2.get(idx)) / norm);
        }
        return conflation;
    }
    
    public static <I> Map<I, Double> softMax(Map<I, Double> map)
    {
        double sumTerm = map
            .values()
            .stream()
            .mapToDouble(d -> Math.exp(d))
            .sum();
        
        return map
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> e.getKey(),
                e -> Math.exp(e.getValue()) / sumTerm));
    
    }
}
