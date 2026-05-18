package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.RepairRuntimeException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.SetOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * An aggregate cost function provides a method to combine several simple 
 * IterableCostFunctions into a composed function.
 * 
 * Weights can be used to active one cost function more than another.
 * @author abronsel
 * @param <T>
 */
public class AggregateCostFunction<T> implements CostFunction<T>
{   
    public static final String SUM      = "sum";
    public static final String MIN      = "min";
    public static final String MAX      = "max";
    public static final String PRODUCT  = "product";
    
    private final Map<CostFunction<T>, Integer> basicCostFunctions;
    
    private final BinaryOperator<Integer> aggregator;
    
    private final String aggregatorName;

    private AggregateCostFunction(Map<CostFunction<T>, Integer> basicCostFunctions, BinaryOperator<Integer> aggregator, String aggregatorName)
    {
        this.basicCostFunctions = Objects.requireNonNullElseGet(basicCostFunctions, HashMap::new);

        this.aggregator = aggregator;
        this.aggregatorName = aggregatorName;

        //Check 1: check if weights are all positive
        if(basicCostFunctions.values().stream().anyMatch(w -> w < 0))
            throw new RepairRuntimeException("Weights must be positive");
        
        //Check 2: check if some of weights is greater than zero 
        if(basicCostFunctions.values().stream().reduce(0, Integer::sum) == 0)
            throw new RepairRuntimeException("Sum of weights must be greater than zero");
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.basicCostFunctions);
        hash = 37 * hash + Objects.hashCode(this.aggregator);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AggregateCostFunction<?> other = (AggregateCostFunction<?>) obj;
        if (!Objects.equals(this.basicCostFunctions, other.basicCostFunctions)) {
            return false;
        }
        return Objects.equals(this.aggregator, other.aggregator);
    }

    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(basicCostFunctions.isEmpty())
        {
            if(originalValue == null)
                return 1;
        
            if(repairedValue == null)
                return Integer.MAX_VALUE;
        }
        
        int cost = this.basicCostFunctions
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 0) //This is critical: if a weight is zero, we ignore that cost function
            .map(e -> e.getValue() // weight * cost
                    * e.getKey()
                       .computeCost(originalValue, repairedValue, originalObject))
            .reduce(aggregator)
            .orElse(1);

        if (cost < 0) cost = Integer.MAX_VALUE;
        return cost;
    }

    @Override
    public Set<T> alternatives(T originalValue, DataObject originalObject) {
        return this.basicCostFunctions
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 0) //This is critical: if a weight is zero, we ignore that cost function
            .flatMap(e -> e.getKey().alternatives(originalValue, originalObject).stream())
            .collect(Collectors.toSet());
    }

    public Map<CostFunction<T>, Integer> getBasicCostFunctions() {
        return basicCostFunctions;
    }
    
    private static <I> Map<CostFunction<I>, Integer> identityWeights(Set<CostFunction<I>> basicCostFunctions)
    {
        return basicCostFunctions
            .stream()
            .collect(Collectors.toMap(cf -> cf, cf -> 1));
    }

    public String getAggregatorName()
    {
        return aggregatorName;
    }
    
    public static <I> AggregateCostFunction<I> createAggregateBySum(Map<CostFunction<I>, Integer> basicCostFunctions)
    {
        return new AggregateCostFunction<>(basicCostFunctions, Integer::sum, SUM);
    }
    
    public static <I> AggregateCostFunction<I> createAggregateBySum(Set<CostFunction<I>> basicCostFunctions)
    {
        return createAggregateBySum(identityWeights(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateBySum(CostFunction<I> ... basicCostFunctions)
    {
        return createAggregateBySum(SetOperations.set(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByProduct(Map<CostFunction<I>, Integer> basicCostFunctions)
    {
        return new AggregateCostFunction<>(basicCostFunctions, (i,j) -> i*j, PRODUCT);
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByProduct(Set<CostFunction<I>> basicCostFunctions)
    {
        return createAggregateByProduct(identityWeights(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByProduct(CostFunction<I> ... basicCostFunctions)
    {
        return createAggregateByProduct(SetOperations.set(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMinimum(Map<CostFunction<I>, Integer> basicCostFunctions)
    {
        return new AggregateCostFunction<>(basicCostFunctions, Integer::min,MIN);
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMinimum(Set<CostFunction<I>> basicCostFunctions)
    {
        return createAggregateByMinimum(identityWeights(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMinimum(CostFunction<I> ... basicCostFunctions)
    {
        return createAggregateByMinimum(SetOperations.set(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMaximum(Map<CostFunction<I>, Integer> basicCostFunctions)
    {
        return new AggregateCostFunction<>(basicCostFunctions, Integer::max, MAX);
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMaximum(Set<CostFunction<I>> basicCostFunctions)
    {
        return createAggregateByMaximum(identityWeights(basicCostFunctions));
    }
    
    public static <I> AggregateCostFunction<I> createAggregateByMaximum(CostFunction<I> ... basicCostFunctions)
    {
        return createAggregateByMaximum(SetOperations.set(basicCostFunctions));
    }
}