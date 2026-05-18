package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;

import java.util.function.BiPredicate;

public class PredicateCostFunction<T extends Comparable<? super T>> implements CostFunction<T> {

    private final BiPredicate<T,T> predicate;

    private final CostFunction<T> costFunctionWhenTrue;
    private final CostFunction<T> costFunctionWhenFalse;

    public PredicateCostFunction(BiPredicate<T,T>  predicate, CostFunction<T> costFunctionWhenTrue, CostFunction<T> costFunctionWhenFalse) {
        this.predicate = predicate;
        this.costFunctionWhenTrue = costFunctionWhenTrue;
        this.costFunctionWhenFalse = costFunctionWhenFalse;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {
        if (repairedValue == null) {
            return Integer.MAX_VALUE;
        }

        return predicate.test(originalValue, repairedValue)
                ? costFunctionWhenTrue.computeCost(originalValue, repairedValue, originalObject)
                : costFunctionWhenFalse.computeCost(originalValue, repairedValue, originalObject);
    }
}
