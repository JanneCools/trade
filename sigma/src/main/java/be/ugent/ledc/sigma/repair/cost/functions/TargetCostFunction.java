package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Interval;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;

/**
 * A cost function that uses a target value to assess the goodness of the repair value.
 * @author abronsel
 * @param <T>
 * @param <C>
 */
public class TargetCostFunction <T extends Comparable<? super T>, C extends OrdinalContractor<T>> implements CostFunction<T>
{
    private final C contractor;

    private final T target;

    public TargetCostFunction(C contractor, T target)
    {
        this.contractor = contractor;
        this.target = contractor.get(target);
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(repairedValue == null)
            return Integer.MAX_VALUE;

        if(originalValue == null)
            return 1;

        return target.compareTo(repairedValue) <= 0
                ? (int) contractor.cardinality(new Interval<>(target, repairedValue)) + 1
                : (int) contractor.cardinality(new Interval<>(repairedValue, target)) + 1;
    }

    public C getContractor() {
        return contractor;
    }

    public T getTarget() {
        return target;
    }
}
