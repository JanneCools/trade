package be.ugent.ledc.chronos.algorithms.currency.curby.distribution;

import be.ugent.ledc.core.operators.UnitScore;
import java.util.Map;

public class IntProbTable extends ProbabilityTable<Integer> implements IntDistribution{
    
    public IntProbTable(Map<Integer, UnitScore> probabilities)
    {
        super(probabilities);
    }
    
}
