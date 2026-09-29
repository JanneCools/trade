package core.cost;

import core.dataset.DataObject;
import core.operators.metric.DamerauDistance;
import core.operators.metric.Metric;

public class EditDistanceCostFunction implements CostFunction<String>
{
    private final Metric<Integer, String> editDistance;

    public EditDistanceCostFunction(Metric<Integer, String> editDistance)
    {
        this.editDistance = editDistance;
    }
    
    public EditDistanceCostFunction()
    {
        this(new DamerauDistance());
    }
    
    @Override
    public int computeCost(String originalValue, String repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
            return 1;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        return editDistance.distance(originalValue, repairedValue);
    }
}
