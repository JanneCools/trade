package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.function.BiPredicate;

/**
 * A Cost function that performs a test on original and repair value and determines
 * the cost based on the outcome of that test
 * @author abronsel
 * @param <T>
 */
public class PredicateCostFunction<T extends Comparable<? super T>> implements CostFunction<T>
{
    private final int costWhenTrue;
    
    private final int costWhenFalse;
    
    private final BiPredicate<T,T> repairTest;

    public PredicateCostFunction(int costWhenTrue, int costWhenFalse, BiPredicate<T, T> repairTest) {
        this.costWhenTrue = costWhenTrue;
        this.costWhenFalse = costWhenFalse;
        this.repairTest = repairTest;
    }

    public PredicateCostFunction(int costWhenFalse, BiPredicate<T, T> repairTest) {
        this(1, costWhenFalse, repairTest);
    }

    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if(originalValue == null)
            return 1;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        return repairTest.test(originalValue, repairedValue)
            ? costWhenTrue
            : costWhenFalse;
    }
    
}
