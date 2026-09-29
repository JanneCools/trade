package core.cost;

import core.dataset.DataObject;

public class WeightedCostFunction<T> implements CostFunction<T>
{
    private final String weightAttribute;
    
    public WeightedCostFunction(String wAttribute)
    {
        this.weightAttribute = wAttribute;
    }
    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject o)
    {
        return o.get(weightAttribute) == null
            ? 1
            : o.getInteger(weightAttribute);
    }

    public String getWeightAttribute()
    {
        return weightAttribute;
    }
    
    

}
