package be.ugent.ledc.sigma.repair.cost.iterators;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.TreeMap;

public class SimpleRepairNavigator<T extends Comparable<? super T>> implements ForwardNavigator<T>
{
    private final TreeMap<T, Integer> navigatorMap;

    public SimpleRepairNavigator(T original, TreeMap<T, Integer> baseValues, int defaultCost, ValueIterator<T> permittedValues) throws RepairException
    {
        //Make a set to support navigation, with the same comparator as the base values
        this.navigatorMap = new TreeMap<>(baseValues.comparator());
            
        //Sanity check: costs must be positive
        if(baseValues.values().stream().anyMatch(cost -> cost < 0))
            throw new RepairException("Cost functions must be positive definite. Found: " + baseValues);
        
        if(baseValues.entrySet().stream().anyMatch(e -> e.getValue() == 0 && !e.getKey().equals(original)))
            throw new RepairException("Cost functions must be positive definite. Found: " + baseValues);
        
        //Sanity check: defaultCost must be higher than any specified cost
        if(baseValues.values().stream().anyMatch(cost -> cost > defaultCost))
            throw new RepairException("A simple repair navigator requires the default cost to be higher than any specified cost. Found default cost " + defaultCost + " and mapping:\n" + baseValues);
        
        //Step 1: put all elements from the base map
        navigatorMap.putAll(baseValues);
        
        //Step 2: remove values that are not permitted
        navigatorMap.keySet().removeIf(value -> !permittedValues.inSelection(value));

        //Step 3: each permitted value not yet added is added with the default cost
        for (T next : permittedValues) {
            //Only add next if the value is not yet in the navigator set
            if (!baseValues.containsKey(next) && (original == null || !original.equals(next))) {
                baseValues.put(next, defaultCost);
                navigatorMap.put(next, defaultCost);
            }
        }
    }
    
    @Override
    public boolean hasNext(T current)
    {
        return navigatorMap.higherKey(current) != null;
    }

    @Override
    public T next(T current)
    {
        return navigatorMap.higherKey(current);
    }

    @Override
    public T first()
    {
        return navigatorMap.isEmpty() ? null : navigatorMap.firstKey();
    }
    
}
