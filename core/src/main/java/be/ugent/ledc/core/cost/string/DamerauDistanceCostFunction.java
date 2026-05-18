package be.ugent.ledc.core.cost.string;

import be.ugent.ledc.core.cost.EditDistanceCostFunction;
import be.ugent.ledc.core.operators.metric.DamerauDistance;

public class DamerauDistanceCostFunction extends EditDistanceCostFunction
{
    public DamerauDistanceCostFunction()
    {
        super(new DamerauDistance());
    }
}
