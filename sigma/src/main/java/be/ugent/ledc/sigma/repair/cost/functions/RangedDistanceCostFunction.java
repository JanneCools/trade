package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

public class RangedDistanceCostFunction <T extends Comparable<? super T>, C extends OrdinalContractor<T>> implements CostFunction<T>
{
    private final C contractor;
    private final T minValue;
    private final T maxValue;

    public RangedDistanceCostFunction(C contractor, T minValue, T maxValue) {
        this.contractor = contractor;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null || originalValue.compareTo(minValue) < 0 || originalValue.compareTo(maxValue) > 0)
            return 1;

        if(repairedValue == null)
            return Integer.MAX_VALUE;

        return originalValue.compareTo(repairedValue) <= 0
                ? (int) contractor.cardinality(new Interval<>(originalValue, repairedValue)) + 1
                : (int) contractor.cardinality(new Interval<>(repairedValue, originalValue)) + 1;
    }

    public C getContractor()
    {
        return contractor;
    }

}
