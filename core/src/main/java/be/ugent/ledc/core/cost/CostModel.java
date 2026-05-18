package be.ugent.ledc.core.cost;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class CostModel<F extends CostFunction> {

    private final Map<String, F> costFunctions;

    public CostModel(Map<String, F> costFunctions) {
        this.costFunctions = costFunctions;
    }

    public CostModel() {
        this(new HashMap<>());
    }

    public Map<String, F> getCostFunctions() {
        return costFunctions;
    }

    public F getCostFunction(String attribute) {
        return costFunctions.get(attribute);
    }
    
    public CostModel<F> addCostFunction(String attribute, F costFunction) {
        this.costFunctions.put(attribute, costFunction);
        return this;
    }

    public Set<String> getAttributes() {
        return new HashSet<>(costFunctions.keySet());
    }
    
    /**
     * Compute the cost of changing an original object into a modified object
     * @param original
     * @param modified
     * @return 
     */
    public int cost(DataObject original, DataObject modified)
    {
        //If one cost function has infinity cost, the total cost is infinite
        if(original.getAttributes()
            .stream()
            .filter(a -> getCostFunction(a) != null)
            .mapToInt(a -> getCostFunction(a)
                .computeCost(original.get(a), modified.get(a),original))
            .anyMatch(cost -> cost == Integer.MAX_VALUE)
        )
        
            return Integer.MAX_VALUE;
        //If one cost function has infinity cost, the total cost is infinite
        if(modified
            .getAttributes()
            .stream()
            .filter(a -> !original.getAttributes().contains(a))
            .filter(a -> getCostFunction(a) != null)
            .mapToInt(a -> getCostFunction(a)
                .computeCost(null, modified.get(a),original))
            .anyMatch(cost -> cost == Integer.MAX_VALUE)
        )
            return Integer.MAX_VALUE;
        
        return original.getAttributes()
            .stream()
            .filter(a -> getCostFunction(a) != null)
            .mapToInt(a -> getCostFunction(a)
                .cost(original.get(a), modified.get(a),original))
            .sum()
            +
             modified
            .getAttributes()
            .stream()
            .filter(a -> !original.getAttributes().contains(a))
            .filter(a -> getCostFunction(a) != null)
            .mapToInt(a -> getCostFunction(a).computeCost(
                null,
                modified.get(a),
                original))
            .sum();
            
    }
}
