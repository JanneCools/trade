package core.cost.string;

import core.cost.EditDistanceCostFunction;
import core.operators.metric.DamerauDistance;

public class DamerauDistanceCostFunction extends EditDistanceCostFunction
{
    public DamerauDistanceCostFunction()
    {
        super(new DamerauDistance());
    }
}
