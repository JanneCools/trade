package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.function.Predicate;

public class ContextTestCostFunction<T extends Comparable<? super T>> implements CostFunction<T>
{
    private final int costWhenTrue;
    
    private final int costWhenFalse;
    
    private final Predicate<DataObject> contextTest;

    public ContextTestCostFunction(int costWhenTrue, int costWhenFalse, Predicate<DataObject> contextTest) {
        this.costWhenTrue = costWhenTrue;
        this.costWhenFalse = costWhenFalse;
        this.contextTest = contextTest;
    }

    public ContextTestCostFunction(int costWhenFalse, Predicate<DataObject> contextTest) {
        this(1, costWhenFalse, contextTest);
    }
    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if(originalValue == null)
            return 1;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        return contextTest.test(originalObject)
            ? costWhenTrue
            : costWhenFalse;
    }
}
