package be.ugent.ledc.sigma.repair.covers;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetOperations;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.NullBehavior;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;

import java.util.*;
import java.util.stream.Collectors;

public class MinimalCoverSearch implements CoverSearch
{
    private final SCCSigmaRuleset ruleset;
    
    private final ConstantCostModel costModel;
    
    public MinimalCoverSearch(SCCSigmaRuleset ruleset, ConstantCostModel costModel)
    {
        this.ruleset = ruleset;
        this.costModel = costModel;
    }

    @Override
    public Set<Set<String>> findMinimalCovers(DataObject object, NullBehavior nullBehavior)
    {

        Map<SigmaRuleset, List<List<String>>> minimalCoveringSetsPerPartition = new HashMap<>();

        Set<SigmaRuleset> partitions = SigmaRulesetOperations.partition(ruleset);

        for (SigmaRuleset partition :  partitions) {

            //Get violated rules
            Set<SigmaRule> violatedRules = partition.failingRules(object);

            if(!violatedRules.isEmpty()) {

                //Get minimal covering sets for these rules, accounting for the cost model
                Set<Set<String>> minimalCoveringSets = new HashSet<>();

                searchMinimalCovers(new HashSet<>(), violatedRules, minimalCoveringSets);

                minimalCoveringSetsPerPartition.put(partition, minimalCoveringSets.stream().map(ArrayList::new).collect(Collectors.toList()));

            }

        }

        Set<Set<String>> result = combineMinimalCoversPerPartition(minimalCoveringSetsPerPartition);

        //We check if there are attributes with NULL values that need to be added to the solution
        Set<String> toAdd = object.getAttributes()
            .stream()
            .filter(a -> object.get(a) == null)
            .filter(a -> nullBehavior.repairIfNull(ruleset, a))
            .collect(Collectors.toSet());


        if(!toAdd.isEmpty())
        {
            result.forEach((mcs) -> mcs.addAll(toAdd));
        }

        return result;

    }
    
    private void searchMinimalCovers(Set<String> current, Set<SigmaRule> nonCoveredRules, Set<Set<String>> minimalCoveringSets)
    {
        if(nonCoveredRules.isEmpty())
        {
            minimalCoveringSets.add(current);
            return;
        }
        
        Set<String> attributes = nonCoveredRules
            .stream()
            .flatMap(r -> r.getInvolvedAttributes().stream())
            .collect(Collectors.toSet());
        
        //Loop over all attributes
        for (String attribute: attributes)
        {
            //Get all rules that don't involve this attribute
            Set<SigmaRule> newNonCoveredRules = nonCoveredRules
                .stream()
                .filter(r -> !r.involves(attribute))
                .collect(Collectors.toSet());
            
            //Construct a new current solution
            Set<String> next = new HashSet<>(current);
            next.add(attribute);
            
            //Check if the current solution + attribute covers all violated rules
            if(newNonCoveredRules.isEmpty())
            {
                //Check if the solution is minimal
                Integer solutionCost = costModel.cost(next);
                
                Integer currentCost = minimalCoveringSets.isEmpty()
                    ? Integer.MAX_VALUE
                    : costModel.cost(minimalCoveringSets.stream().findFirst().get());
                
                if(solutionCost <= currentCost)
                {
                    if(solutionCost < currentCost)
                    {
                        minimalCoveringSets.clear();
                    }
                    
                    if(minimalCoveringSets.stream().noneMatch(sol -> next.containsAll(sol)))
                    {
                        minimalCoveringSets.add(next);
                    }
                }
            }
            else
            {                
                searchMinimalCovers(next, newNonCoveredRules, minimalCoveringSets);
            }
        }
    }

    private Set<Set<String>> combineMinimalCoversPerPartition(Map<SigmaRuleset, List<List<String>>> minimalCoveringSetsPerPartition) {

        Set<Set<String>> combinations = new HashSet<>();

        List<SigmaRuleset> keys = new ArrayList<>(minimalCoveringSetsPerPartition.keySet());

        // Number of partitions
        int n = keys.size();

        // To keep track of next set cover in each of the n partitions
        int[] indices = new int[n];

        // Initialize with first set cover's index
        for(int i = 0; i < n; i++) {
            indices[i] = 0;
        }

        while (true) {

            Set<String> currentCombination = new HashSet<>();

            // Print current combination
            for (int i = 0; i < n; i++) {
                currentCombination.addAll(minimalCoveringSetsPerPartition.get(keys.get(i)).get(indices[i]));
            }

            combinations.add(currentCombination);

            // Find the rightmost partition that has more set covers left after the current set cover in that partition
            int next = n - 1;

            while(next >= 0 && indices[next] + 1 >= minimalCoveringSetsPerPartition.get(keys.get(next)).size()) {
                next--;
            }

            // No such partition is found, so no more combinations left
            if (next < 0) {
                break;
            }

            // If found move to next set cover in that partition
            indices[next]++;

            // For all partitions to the right of this partition, the current index again points to first set cover
            for (int i = next + 1; i < n; i++) {
                indices[i] = 0;
            }


        }

        return combinations;

    }
    
}
