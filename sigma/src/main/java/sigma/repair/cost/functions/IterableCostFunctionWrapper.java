package sigma.repair.cost.functions;

import core.dataset.DataObject;
import core.cost.CostFunction;
import java.util.Set;

public class IterableCostFunctionWrapper<T extends Comparable<? super T>> extends AbstractIterableCostFunction<T>
{
    private final CostFunction<T> wrappedCostFunction;

    public IterableCostFunctionWrapper(CostFunction<T> wrappedCostFunction)
    {
        this.wrappedCostFunction = wrappedCostFunction;
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        return wrappedCostFunction.computeCost(originalValue, repairedValue, originalObject);
    }

    @Override
    public Set<T> alternatives(T originalValue, DataObject originalObject)
    {
        return wrappedCostFunction.alternatives(originalValue, originalObject);
    }

    public CostFunction<T> getWrappedCostFunction() {
        return wrappedCostFunction;
    }
}
