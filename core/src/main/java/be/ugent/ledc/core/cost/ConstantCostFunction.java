package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.RepairRuntimeException;

public class ConstantCostFunction<T> implements CostFunction<T>
{
    private final int fixedCost;

    public ConstantCostFunction()
    {
        this.fixedCost = 1;
    }
    
    public ConstantCostFunction(int fixedCost)
    {
        this.fixedCost = fixedCost;
        
        if(fixedCost <= 0)
            throw new RepairRuntimeException("Cost functions must be positive-definite: fixed cost must be strictly greater than 0.");
    }

    @Override
    public int getMinimalCost() { return fixedCost; }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
            return fixedCost;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        return fixedCost;
    }

    public int getFixedCost()
    {
        return fixedCost;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.fixedCost;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConstantCostFunction<?> other = (ConstantCostFunction<?>) obj;
        return this.fixedCost == other.fixedCost;
    }
    
    
}
