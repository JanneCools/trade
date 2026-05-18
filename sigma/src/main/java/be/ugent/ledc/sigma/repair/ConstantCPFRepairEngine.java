package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.CPFRandomRepairSelection;
import be.ugent.ledc.sigma.repair.selection.CPFRepairSelection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConstantCPFRepairEngine extends CPFRepairEngine<ConstantCostModel>
{

    public ConstantCPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, ConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException
    {
        super(cpfs, sigmaRuleset, costModel, nullBehavior, selector);
    }

    public ConstantCPFRepairEngine(Set<CPF> cpfs, SigmaRuleset sigmaRuleset, ConstantCostModel costModel) throws RepairException
    {
        super(cpfs, sigmaRuleset, costModel);
    }

    public ConstantCPFRepairEngine(SigmaRuleset sigmaRuleset, ConstantCostModel costModel, NullBehavior nullBehavior, CPFRepairSelection selector) throws RepairException
    {
        super(sigmaRuleset, costModel, nullBehavior, selector);
    }

    public ConstantCPFRepairEngine(SigmaRuleset sigmaRuleset, ConstantCostModel costModel) throws RepairException
    {
        super(sigmaRuleset, costModel);
    }

    @Override
    public Pair<Integer, Set<CPF>> getMinCostChangeExpressionsWithCost(DataObject dataObject) {

        //Search change expressions
        Map<CPF, Set<CPF>> changeExpressionsMap = super.search(
            dataObject,
            AttributesToKeep.MINIMAL_SETS
        );
        
        Set<CPF> changeExpressions = changeExpressionsMap
            .values()
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        
        //Clean step 1
        changeExpressions
            .removeIf(ce -> ce.getAttributes()
                .stream()
                .anyMatch(at -> getCostModel().getCostFunction(at) == null));
        
        //Clean step 2
        changeExpressions
            .removeIf(ce1 -> changeExpressions
                .stream()
                .anyMatch(ce2 -> !ce1.equals(ce2) && ce1.implies(ce2)));

        //Data object is clean: require no changes
        if (changeExpressions.isEmpty())
            return new Pair<>(0, new HashSet<>());

        Set<CPF> minCostChangeExpressions = new HashSet<>();
        
        int minCost = Integer.MAX_VALUE;

        for (CPF changeExpression : changeExpressions) {

            Set<String> involvedAttributes = changeExpression.getAttributes();
            int currentCost = involvedAttributes.stream().mapToInt(ia -> getCostModel().getCostFunction(ia).getFixedCost()).sum();

            if (currentCost < minCost) {
                minCostChangeExpressions = new HashSet<>();
                minCostChangeExpressions.add(changeExpression);
                minCost = currentCost;
            } else if (minCost == currentCost) {
                minCostChangeExpressions.add(changeExpression);
            }

        }

        return new Pair<>(minCost, minCostChangeExpressions);

    }
}
