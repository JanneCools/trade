package be.ugent.ledc.sigma.repair.cost.models;

import be.ugent.ledc.core.cost.ConstantCostFunction;
import be.ugent.ledc.core.cost.CostModel;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConstantCostModel extends CostModel<ConstantCostFunction> {

    public ConstantCostModel(Map<String, ConstantCostFunction> costFunctions) {
        super(costFunctions);
    }

    public ConstantCostModel(Set<String> attributes) {
        this(attributes
                .stream()
                .distinct()
                .collect(Collectors.toMap(
                        a -> a,
                        a -> new ConstantCostFunction<>())
                ));
    }

    public ConstantCostModel(){}

    public ConstantCostModel addConstantCostFunction(String attribute, Integer constantCost) {
        super.addCostFunction(attribute, new ConstantCostFunction<>(constantCost));
        return this;
    }

    public Integer cost(Set<String> solution)
    {
        //If one attribute of the solution has no cost function, this solution is
        //assigned infinite cost.
        if(solution.stream().anyMatch(sa -> getCostFunction(sa) == null))
            return Integer.MAX_VALUE;

        return solution
                .stream()
                .mapToInt(a -> getCostFunction(a).getFixedCost())
                .sum();
    }
}
