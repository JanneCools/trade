package sigma.repair.cost.functions;

import core.dataset.DataObject;
import sigma.datastructures.contracts.ForwardNavigator;
import sigma.datastructures.values.ValueIterator;
import sigma.repair.cost.iterators.SortedSetRepairNavigator;
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
