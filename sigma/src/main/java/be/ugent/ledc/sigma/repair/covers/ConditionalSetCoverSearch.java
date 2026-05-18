package be.ugent.ledc.sigma.repair.covers;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.NullBehavior;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A CoverSearch method where some attributes are forced to keep their values.
 * The covers that are found are subset minimal, meaning that for each cover,
 * no subset is also a cover
 * @author abronsel
 */
public class ConditionalSetCoverSearch implements CoverSearch
{
    private final SCCSigmaRuleset ruleset;
    
    private final Set<String> conditions;
    
    public ConditionalSetCoverSearch(SCCSigmaRuleset ruleset, Set<String> conditions)
    {
        this.ruleset = ruleset;
        this.conditions = conditions;
    }
    
    public ConditionalSetCoverSearch(SCCSigmaRuleset ruleset)
    {
        this(ruleset, new HashSet<>());
    }

    @Override
    public Set<Set<String>> findMinimalCovers(DataObject object, NullBehavior nullBehavior)
    {
        //Get violated rules
        Set<SigmaRule> failingRules = ruleset.failingRules(object);
        
        //Get minimal covering sets for these rules, accounting for the cost model
        Set<Set<String>> minimalCoveringSets = new HashSet<>();
        
        //Sanity check: if a failing rule involves only attributes that are conditional
        //the conditions are too strict and we can NEVER find a cover
        if(failingRules
            .stream()
            .anyMatch(rule -> rule
                .getInvolvedAttributes()
                .stream()
                .allMatch(a -> conditions.contains(a))
            )
        )
        {
            return new HashSet<>();
        }
        
        searchMCS(new HashSet<>(), failingRules, minimalCoveringSets);
        
        minimalCoveringSets
            .removeIf(sol -> minimalCoveringSets //Remove a solution if
                .stream()
                .anyMatch(subSol -> sol .containsAll(subSol)// Another solution is included
                    && subSol.size() < sol.size())); //and has strictly less attributes
        
        //We check if there are attributes with NULL values that
        //need to be added to the solution      
        Set<String> toAdd = object.getAttributes()
            .stream()
            .filter(a -> !conditions.contains(a))
            .filter(a -> object.get(a) == null)
            .filter(a -> nullBehavior.repairIfNull(ruleset, a))
            .collect(Collectors.toSet());

        if(!toAdd.isEmpty())
            minimalCoveringSets.forEach((mcs) -> mcs.addAll(toAdd));
        

        //Return
        return  minimalCoveringSets;
    }
    
    private void searchMCS(Set<String> current, Set<SigmaRule> nonCoveredRules, Set<Set<String>> minimalCoveringSets)
    {
        if(nonCoveredRules.isEmpty())
        {
            minimalCoveringSets.add(current);
            return;
        }
        
        List<String> toCheck = nonCoveredRules
            .stream()
            .flatMap(r -> r.getInvolvedAttributes().stream())
            .filter(a -> !conditions.contains(a)) // A conditional attribute cannot be in a cover
            .distinct()
            .sorted(Comparator.<String,Long>comparing((a) ->
                    nonCoveredRules
                        .stream()
                        .filter(rule -> rule.involves(a))
                        .count()
                ).reversed()
            )
            .collect(Collectors.toList());
        
        //Loop over all attributes
        for (String attribute: toCheck)
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
                minimalCoveringSets.add(next);
            }
            else
            {                
                searchMCS(next, newNonCoveredRules, minimalCoveringSets);
            }
        }
    }
}
