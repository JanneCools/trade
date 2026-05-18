package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.cost.errormechanism.ErrorMechanism;
import java.util.HashSet;
import java.util.Set;

public class ErrorFunction<T> implements CostFunction<T>
{
    /**
     * An error mechanism embedded in the cost function
     * by the error mechanism
     */
    private final ErrorMechanism<T> errorMechanism;
    
    /**
     * The cost when the value cannot be explained
     */
    private final int nonExplainedCost;
    
    public ErrorFunction(int nonExplainedCost, ErrorMechanism<T> errorMechanism)
    {
        this.nonExplainedCost = nonExplainedCost;
        this.errorMechanism = errorMechanism;
    }
    
    public ErrorFunction(ErrorMechanism<T> errorMechanism)
    {
        this(2, errorMechanism);
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
            return nonExplainedCost;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        return errorMechanism.explains(originalValue, repairedValue, originalObject)
            ? 1
            : nonExplainedCost;
            
    }
    
    public ErrorMechanism<T> getErrorMechanism()
    {
        return errorMechanism;
    }

    public int getNonExplainedCost()
    {
        return nonExplainedCost;
    }

    @Override
    public Set<T> alternatives(T originalValue, DataObject originalObject)
    {
        return new HashSet<>(errorMechanism.explanations(originalValue, originalObject));
    }

}
