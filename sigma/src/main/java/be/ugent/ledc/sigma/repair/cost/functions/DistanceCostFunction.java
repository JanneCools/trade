package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

public class DistanceCostFunction <T extends Comparable<? super T>, C extends OrdinalContractor<T>> implements CostFunction<T>
{
    private final C contractor;

    public DistanceCostFunction(C contractor)
    {
        this.contractor = contractor;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
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
