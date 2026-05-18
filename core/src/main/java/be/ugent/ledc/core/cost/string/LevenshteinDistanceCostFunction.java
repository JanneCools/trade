package be.ugent.ledc.core.cost.string;

import be.ugent.ledc.core.cost.EditDistanceCostFunction;
import be.ugent.ledc.core.operators.metric.LevenshteinDistance;

public class LevenshteinDistanceCostFunction extends EditDistanceCostFunction
{
    public LevenshteinDistanceCostFunction()
    {
        super(new LevenshteinDistance());
    }
}
