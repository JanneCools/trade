package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public interface CostFunction<T>
{
    default int cost(T originalValue, T repairedValue, DataObject dataObject) {
        if (Objects.equals(originalValue, repairedValue))
            return 0;

        return computeCost(originalValue, repairedValue, dataObject);
    }

    int computeCost(T originalValue, T repairedValue, DataObject originalObject);
    
    default Set<T> alternatives(T originalValue, DataObject originalObject)
    {
        return new HashSet<>();
    }

    default int getMinimalCost() { return 1; }
}
