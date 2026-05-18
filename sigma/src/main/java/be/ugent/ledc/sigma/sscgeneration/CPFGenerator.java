package be.ugent.ledc.sigma.sscgeneration;

import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.AtomImplicator;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.formulas.CPFImplicator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetInverter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An SCC generator for SigmaRules that internally converts SigmaRules to CPFs
 * and performs a full implication of atom pairs. The SCC set is obtained by
 * converting the CPFs back into an equivalent SigmaRule representation.
 * @author abronsel
 */
public class CPFGenerator implements SCCGenerator<SigmaRule, SigmaRuleset>{

    @Override
    public Set<SigmaRule> generateSCCSet(SigmaRuleset ruleset)
    {
        Set<CPF> cpfs = SigmaRulesetInverter.invert(ruleset);
        
        int cpfIndex = 1;
        
        
        Map<Integer, AbstractAtom<?,?,?>> atomIndex = new HashMap<>();
        Map<Integer, Set<Integer>> ruleIndex = new HashMap<>();
        
        for(CPF cpf: cpfs)
        {
            for(AbstractAtom aa: cpf.getAtoms())
            {
                Set<String> attrs = aa
                    .getAttributes()
                    .collect(Collectors.toSet());
                
                Integer key = atomIndex
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().getAttributes().allMatch(attrs::contains))
                    .filter(e -> e.getValue().equals(aa) ||
                        (AtomImplicator.implies(e.getValue(), aa) && AtomImplicator.implies(aa, e.getValue())))
                    .map(e -> e.getKey())
                    .findFirst()
                    .orElse(null);
                
                if(key == null)
                {
                    int idx = atomIndex.size() + 1;
                    atomIndex.put(idx, aa);
                    ruleIndex.put(idx, SetOperations.set(cpfIndex));
                }
                else
                {
                    ruleIndex.get(key).add(cpfIndex);
                }
            }
            
            cpfIndex++;
        }
        
        Set<Set<Integer>> covers = new HashSet<>();
        
        searchCovers(
            covers,
            ruleIndex
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        e -> SetOperations.set(e.getKey()),
                        e -> e.getValue()
                    )
                ),
            1,
            cpfs.size());
        
        
        Set<SigmaRule> rules = covers
            .stream()
            .map(cover -> new SigmaRule(
                CPFImplicator.imply(new CPF(cover
                    .stream()
                    .map(idx -> atomIndex.get(idx).getInverse())
                    .collect(Collectors.toSet()
                )))
            ))
            .collect(Collectors.toSet());
        
        rules.removeIf(rule -> rules
            .stream()
            .anyMatch(dRule -> !dRule.equals(rule) && rule.isRedundantTo(dRule)));
        
        return rules;
    }

    private void searchCovers(Set<Set<Integer>> covers,  Map<Set<Integer>, Set<Integer>> ruleIndex, int current, int cpfSize)
    {
        if(ruleIndex.isEmpty())
            return;
        
        //Check for output
        for(Set<Integer> cover: ruleIndex.keySet())
        {
            //If an atom combination covers all CPFs, add it to the cover
            if(ruleIndex.get(cover).size() == cpfSize)
            {
                covers.add(cover);
            }
        }
        
        //Remove full covers
        ruleIndex.entrySet().removeIf(e -> e.getValue().size() == cpfSize);
        
        //Construct new rule index
        Map<Set<Integer>, Set<Integer>> newRuleIndex = new HashMap<>();
        
        for(Set<Integer> leftCover: ruleIndex.keySet())
        {
            for(Set<Integer> rightCover: ruleIndex.keySet())
            {
                if(ruleIndex.get(leftCover).containsAll(ruleIndex.get(rightCover)))
                    continue;
                
                if(ruleIndex.get(rightCover).containsAll(ruleIndex.get(leftCover)))
                    continue;
                
                Set<Integer> union = SetOperations.union(leftCover, rightCover);
                
                if(union.size() != current + 1)
                    continue;
                
                newRuleIndex.put(
                    union,
                    SetOperations.union(
                        ruleIndex.get(leftCover),
                        ruleIndex.get(rightCover))
                );
                
            }
        }
        
        //Recursive call
        searchCovers(
            covers,
            newRuleIndex,
            current+1,
            cpfSize);
    }
}
