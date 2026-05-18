package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.function.Function;

public class DerivedCostFunction<T> implements CostFunction<T> {

    private final int derivedCost;
    private final int defaultCost;
    private final Function<DataObject, T> derivedValueCalculation;

    public DerivedCostFunction(int derivedCost, int defaultCost, Function<DataObject, T> derivedValueCalculation) {
        this.derivedCost = derivedCost;
        this.defaultCost = defaultCost;
        this.derivedValueCalculation = derivedValueCalculation;
    }

    public DerivedCostFunction(int derivedCost, Function<DataObject, T> derivedValueCalculation) {
        this.derivedCost = derivedCost;
        this.defaultCost = derivedCost + 1;
        this.derivedValueCalculation = derivedValueCalculation;
    }

    public int getDerivedCost() {
        return derivedCost;
    }

    public int getDefaultCost() {
        return defaultCost;
    }

    public Function<DataObject, T> getDerivedValueCalculation() {
        return derivedValueCalculation;
    }

    @Override
    public int getMinimalCost() {
        return Math.min(derivedCost, defaultCost);
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject) {

        if (repairedValue == null)
            return Integer.MAX_VALUE;

        try {
            T result = derivedValueCalculation.apply(originalObject);
            return result.equals(repairedValue) ? derivedCost : defaultCost;
        } catch (RuntimeException ex) {
            return defaultCost;
        }

    }

}
