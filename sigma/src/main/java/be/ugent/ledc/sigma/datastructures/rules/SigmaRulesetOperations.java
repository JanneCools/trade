package be.ugent.ledc.sigma.datastructures.rules;

import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.AtomOperations;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SigmaRulesetOperations
{
    /**
     * Partitions a given SigmaRuleset into Rulesets that are independently from one another.
     * Such independent sets have the property that Sufficient sets can be found
     * for each independent set separately. Consequently, also in repairing, they can often
     * be treated separately, which provides a gain in computational complexity.
     * @param ruleset
     * @return
     */
    public static Set<SigmaRuleset> partition(SigmaRuleset ruleset)
    {
        List<String> listOfAttributes = ruleset
            .getContractors()
            .keySet()
            .stream()
            .toList();

        //Map a unique index to each singleton set of attributes
        Map<Integer, Set<String>> indexMap = IntStream
            .range(0, listOfAttributes.size())
            .boxed()
            .collect(
                Collectors.toMap(
                    i -> i,
                    i -> Stream
                        .of(listOfAttributes.get(i))
                        .collect(Collectors.toSet())
                )
            );

        for(SigmaRule rule: ruleset.getRules())
        {
            if(rule.getInvolvedAttributes().size() > 1)
            {
                //Find indices to merge
                Set<Integer> indicesToMerge = indexMap
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue()
                            .stream()
                            .anyMatch(rule::involves))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
                
                //Compute the minimum of the indices
                int mergeIndex = Collections.min(indicesToMerge);

                //Merge
                for(Integer index: indicesToMerge)
                {
                    //Merge attribute sets, if index != mergeIndex
                    if(index != mergeIndex)
                    {
                        indexMap.get(mergeIndex).addAll(indexMap.get(index));
                        indexMap.remove(index);
                    }
                }
            }
        }

        return indexMap
            .values()
            .stream()
            .map(ruleset::project) //Project
            .collect(Collectors.toSet());

    }
    
    public static Set<String> convertUnivariateRulesToSet(Set<SigmaRule> univariateRules, String a)
    {
        //Check if univariate rules only
        if(univariateRules.stream().flatMap(rule -> rule.getInvolvedAttributes().stream()).distinct().count() != 1)
        {
            throw new SigmaRuleException("Cannot convert univariate rules to set."
                + "Cause: rules involve multiple attributes");
        }
        
        Set<AbstractAtom<?,?,?>> atoms = new HashSet<>();
            
        for(SigmaRule uRule: univariateRules)
        {
            AbstractAtom<?, ?, ?> atom;

            if(uRule.getAtoms().size() == 1)
            {
                atom = uRule
                    .getAtoms()
                    .stream()
                    .findFirst()
                    .get();
            }
            else
            {
                atom = CPFImplicator
                    .imply(uRule.getCPF())
                    .getAtoms()
                    .stream()
                    .findFirst()
                    .get();
            }

            atoms.add(atom.getInverse());
        }
        
        return AtomOperations.getValuesetFromNominalAtoms(atoms, a);
    }
}
