package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.Objects;

public class SourceCostFunction<T> implements CostFunction<T>
{
    private final String sourceAttribute;
    
    private final Object sourceValue;
    
    private final int sourceCost;

    public SourceCostFunction(String sourceAttribute, Object sourceValue, int sourceCost)
    {
        this.sourceAttribute = sourceAttribute;
        this.sourceValue = sourceValue;
        this.sourceCost = sourceCost;
    }
    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject o)
    {
        return o.get(sourceAttribute) != null
            && o.get(sourceAttribute).equals(sourceValue) ? sourceCost : 1;
    }

    public String getSourceAttribute()
    {
        return sourceAttribute;
    }

    public Object getSourceValue()
    {
        return sourceValue;
    }

    public int getSourceCost()
    {
        return sourceCost;
    }
    
    
}