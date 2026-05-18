package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.repair.cost.iterators.SimpleRepairNavigator;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SimpleIterableCostFunction<T extends Comparable<? super T>> implements IterableCostFunction<T>
{
    private final HashMap<T, TreeMap<T, Integer>> internalCostMap;
    private final int defaultCost;

    public SimpleIterableCostFunction(Map<T, Map<T, Integer>> costMapping, int defaultCost)
    {
        this.internalCostMap = new HashMap<>();

        //The default cost is one higher than the highest cost
        this.defaultCost = defaultCost;

        // TODO: sanity check: check whether the defaultCost is at least one higher than the highest cost

        //We make sure values are sorted according to their cost
        for(T original: costMapping.keySet())
        {
            Map<T, Integer> unsortedMap = costMapping.get(original);

            // A sanity check that the original cannot appear here, is not possible.
            // In induced cost models (Parker engine), the original might appear here...

            //Sanity check 2: costs must be strictly positive
            if(unsortedMap.values().stream().anyMatch(cost -> cost < 0))
                throw new RepairRuntimeException("Cost functions must be positive definite. Found: " + unsortedMap);

            //Sanity check 3: only original can have cost equal to 0
            if(unsortedMap.entrySet().stream().anyMatch(e -> e.getValue() == 0 && !e.getKey().equals(original)))
                throw new RepairRuntimeException("Cost functions must be positive definite. Found original value " + original + " and " + unsortedMap);

            //We now add sorted mappings
            internalCostMap.put(
                    original,
                    new TreeMap<>(
                            Comparator
                                    .<T,Integer>comparing(value -> unsortedMap.get(value) == null ? defaultCost : unsortedMap.get(value))
                                    .thenComparing(Comparator.naturalOrder())
                    )
            );

            //Now convert the costmappings to pairs and sort them in a set
            internalCostMap.get(original).putAll(unsortedMap);
        }
    }

    public SimpleIterableCostFunction(Map<T, Map<T, Integer>> costMapping)
    {
        this(costMapping, costMapping.isEmpty() ? 1 : costMapping
                .values()
                .stream()
                .flatMapToInt(map -> map
                        .values()
                        .stream()
                        .mapToInt(i->i)
                )
                .max()
                .getAsInt() + 1);
    }

    @Override
    public ForwardNavigator<T> getRepairNavigator(T original, ValueIterator<T> permittedValues, DataObject o) throws RepairException {
        //Build the local mapping of costs for the given original
        TreeMap<T, Integer> localMap = internalCostMap.containsKey(original)
            ? internalCostMap.get(original)
            : new TreeMap<>();

        //Return a simple repair navigator
        return new SimpleRepairNavigator<>(
            original,
            localMap,
            defaultCost,
            permittedValues
        );

    }

    @Override
    public int computeCost(T original, T repaired, DataObject o)
    {
        if(original == null)
            return this.defaultCost;
        
        if(repaired == null)
            return Integer.MAX_VALUE;
        
        if(internalCostMap.containsKey(original) && internalCostMap.get(original).containsKey(repaired))
            return internalCostMap.get(original).get(repaired);
            
        return this.defaultCost;
    }
}
