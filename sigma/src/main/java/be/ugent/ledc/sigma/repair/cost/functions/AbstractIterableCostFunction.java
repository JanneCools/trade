package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.cost.iterators.SortedSetRepairNavigator;
import java.util.Comparator;
import java.util.TreeSet;

public abstract class AbstractIterableCostFunction<T extends Comparable<? super T>> implements IterableCostFunction<T>
{
    @Override
    public ForwardNavigator<T> getRepairNavigator(T originalValue, ValueIterator<T> permittedValues, DataObject originalObject) 
    {
        TreeSet<T> sortedSet = new TreeSet<>(
            Comparator
            .<T, Integer>comparing(v -> cost(originalValue, v, originalObject))
            .thenComparing(Comparator.naturalOrder())
        );

        for (T next : permittedValues) {
            sortedSet.add(next);
        }
        
        return new SortedSetRepairNavigator<>(sortedSet);
    }

}
