package core.cost.string;

import core.cost.EditDistanceCostFunction;
import core.operators.metric.LevenshteinDistance;

public class LevenshteinDistanceCostFunction extends EditDistanceCostFunction
{
    public LevenshteinDistanceCostFunction()
    {
        super(new LevenshteinDistance());
    }
}
