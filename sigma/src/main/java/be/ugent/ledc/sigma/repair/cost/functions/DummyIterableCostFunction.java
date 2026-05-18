package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.cost.iterators.SimpleRepairNavigator;
import java.util.TreeMap;

public class DummyIterableCostFunction<T extends Comparable<? super T>> implements IterableCostFunction<T>
{
    private final int fixedCost;

    public DummyIterableCostFunction()
    {
        this.fixedCost = 1;
    }

    public DummyIterableCostFunction(int fixedCost)
    {
        this.fixedCost = fixedCost;

        if(fixedCost <= 0)
            throw new RepairRuntimeException("Cost functions must be positive-definite: fixed cost must be strictly greater than 0.");
    }

    @Override
    public ForwardNavigator<T> getRepairNavigator(T original, ValueIterator<T> permittedValues, DataObject o) throws RepairException {
        //Return a simple repair navigator
        return new SimpleRepairNavigator<>(
            original,
            new TreeMap<>(),
            fixedCost,
            permittedValues
        );
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
            return fixedCost;

        if(repairedValue == null)
            return Integer.MAX_VALUE;

        return fixedCost;
    }
}
