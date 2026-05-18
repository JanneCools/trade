package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ProbabilityTable<E> implements Distribution<E>
{
    private final TreeMap<E,UnitScore> probabilities;

    public ProbabilityTable(Map<E,UnitScore> probabilities)
    {
        boolean positive = 
            probabilities
            .values()
            .stream()
            .allMatch(pr -> pr.compareTo(UnitScore.ZERO) >= 0);
        
        if(!positive)
            throw new RuntimeException("Invalid distribution. Probabilities must be positive numbers.");
        
//        boolean normalized = 
//            probabilities
//            .values()
//            .stream()
//            .reduce(Prob.ZERO, Prob::add)
//            .compareTo(Prob.ONE) == 0;
//        
//        
//        if(!normalized)
//            throw new RuntimeException("Invalid distribution. Sum of probabilities does not equal one");
        
        this.probabilities = new TreeMap<>();
        this.probabilities.putAll(probabilities);
    }
    
    @Override
    public UnitScore probability(E event)
    {
        if(event == null || !probabilities.containsKey(event))
            return UnitScore.ZERO;
        
        return probabilities.get(event);
    }
    
    public Set<E> support()
    {
        return this.probabilities.keySet();
    }
}
