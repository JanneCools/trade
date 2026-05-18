package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.datastructures.Pair;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.cost.models.IterableCostModel;
import be.ugent.ledc.sigma.repair.covers.SubsetMinimalCoverSearch;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A sequential repair engine that accepts Iterable cost functions and works
 * in a greedy manner. In each step, the subset minimal covers are inspected
 * and for each attribute in such a cover, it is verified how that attribute
 * can be best repaired without changing the non-cover attributes. The best
 * option is then applied and this process continues, but on the condition that
 * the attribute just repaired, cannot be altered anymore.
 * @author abronsel
 */
public class GreedySequentialRepair extends ObjectLevelRepairEngine<IterableCostModel, SCCSigmaRuleset>
{
    /**
     * A set cover search algorithm that finds subset-minimal set covers.
     */
    private final SubsetMinimalCoverSearch setCoverSearch;

    public GreedySequentialRepair(SCCSigmaRuleset rules, IterableCostModel costModel, NullBehavior nullBehavior)
    {
        super(rules, costModel, nullBehavior);
        setCoverSearch = new SubsetMinimalCoverSearch(rules);
    }

    public GreedySequentialRepair(SCCSigmaRuleset rules, IterableCostModel costModel)
    {
        super(rules, costModel);
        setCoverSearch = new SubsetMinimalCoverSearch(rules);
    }
    
    @Override
    public DataObject repair(DataObject dirty) throws RepairException
    {
        if(VERBOSITY >= 1)
            System.out.println("Greedy repairing " + dirty);

        DataObject r = new DataObject(dirty);
        
        Set<String> fixed = dirty
            .getAttributes()
            .stream()
            .filter(a -> getCostModel().getCostFunction(a) == null)
            .collect(Collectors.toSet());
        
        while(getRules().isFailed(r))
        {           
            //Greedy select an attribute
            String a = greedyRepair(
                r,
                fixed,
                dirty);
            
            if(VERBOSITY >= 2)
            {
                System.out.println("-> Changed " + a + " to " + r.get(a));
            }
        }
        
        return r;
    }

    private String greedyRepair(DataObject r, Set<String> fixed, DataObject original) throws RepairException
    {        
        //Find minimal covers
        Set<Set<String>> covers = setCoverSearch.findMinimalCovers(
            r,
            getNullBehavior());
        
        //Remove covers if they contain an attribute that is fixed
        covers.removeIf(cover -> cover.stream().anyMatch(a -> fixed.contains(a)));
        
        if(covers.isEmpty())
            throw new RepairException("No covers found for object " + r);
        
        
        int lowestCost = Integer.MAX_VALUE;
        Pair<String,Object> fix = null;
        
        //For each cover
        for(Set<String> cover: covers)
        {
            //For each attribute
            for(String a: cover)
            {
                //Get selected rules
                SigmaRuleset selectedRules = selectRules(
                    a,
                    cover,
                    r.inverseProject(cover)
                );
                
                //Get permitted values for attribute to treat
                ValueIterator<?> repairValues = RepairOperations
                    .getPermittedValues(
                        a,
                        (Comparable)r.get(a),
                        selectedRules,
                        r.inverseProject(cover)
                    );
                
                for(Object repairValue: repairValues)
                {
                    int cost = getCostModel()
                        .getCostFunction(a)
                        .cost(repairValue, r.get(a), original);
                    
                    if(cost < lowestCost)
                    {
                        lowestCost = cost;
                        fix = new Pair<>(a, repairValue);
                    }
                }
            }
        }
        
        if(fix == null)
            throw new RepairException("Could not repair object "
                + r
                + " in a greedy manner. Cause: best fix is null.");
        
        fixed.add(fix.getFirst());
        r.set(fix.getFirst(), fix.getSecond());
        
        return fix.getFirst();
    }

    private SigmaRuleset selectRules(String attribute, Set<String> toTreat, DataObject repair)
    {
        //We select rules satisfying 3 conditions
        Set<SigmaRule> ruleSelection = getRules()
            .getRules()
            .stream()
            .filter(rule -> rule.involves(attribute)) //1. Rule must involve attribute
            .filter(rule -> toTreat //2. Rule must not involve other attributes to treat
                .stream()
                .allMatch(otherAttribute ->
                    otherAttribute.equals(attribute) ||
                    !rule.involves(otherAttribute)
                ))
            //3. Rules CAN fail after we assign a value for toTreat
            .filter(rule -> rule
                .fix(repair) //If we fix the values of clean object so far...
                .getAtoms()
                .stream()
                .noneMatch(atom -> atom.equals(AbstractAtom.ALWAYS_FALSE))) // ...no atom should be always false
            //In the above, if some atom would be always false, 
            //that rule will always be satisfied regardless of the value
            //we assign to 'attribute'
            .collect(Collectors.toSet());
        
        //We wrap the selected rules in a sigma ruleset and project on the contractors
        return new SigmaRuleset(
        //We project the conctractors: keep only attribute + fixed attributes
        getRules()
            .getContractors()
            .entrySet()
            .stream()
            //.filter(e -> e.getKey().equals(attribute) || !toTreat.contains(e.getKey()))
            .filter(e -> e.getKey().equals(attribute))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            ruleSelection
        );
    }
    
}
