package be.ugent.ledc.sigma.sscgeneration;

import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A factory class to create useful attribute comparators that can be used during
 * FCF generation.
 * @author abronsel
 */
public class AttributeComparatorFactory
{
    /**
     * A comparator in which the order is hand-coded
     * @param fixedOrder
     * @return 
     */
    public static Comparator<String> createFixedComparator(List<String> fixedOrder)
    {
        return (a,b) -> Integer.compare(fixedOrder.indexOf(a), fixedOrder.indexOf(b));
    }
    
    /**
     * A comparator in which attributes that occur in more rules, are put first.
     * @param ruleset
     * @return 
     */
    public static Comparator<String> createHighFrequencyFirstComparator(SigmaRuleset ruleset)
    {
        Map<String,Integer> involvedCountMap = ruleset
            .getContractors()
            .keySet()
            .stream()
            .collect(Collectors.toMap(a->a, a->0));
            
        for(SigmaRule r: ruleset)
        {
            r
            .getInvolvedAttributes()
            .forEach(a -> involvedCountMap.merge(a, 1, Integer::sum));
        }
        
        return (a,b) -> -1 * Integer.compare(involvedCountMap.get(a), involvedCountMap.get(b));
    }
    
    /**
     * A comparator in which attributes that occur in more rules, are put last.
     * @param ruleset
     * @return 
     */
    public static Comparator<String> createLowFrequencyFirstComparator(SigmaRuleset ruleset)
    {
        Map<String,Integer> involvedCountMap = ruleset
            .getContractors()
            .keySet()
            .stream()
            .collect(Collectors.toMap(a->a, a->0));
            
        for(SigmaRule r: ruleset)
        {
            r
            .getInvolvedAttributes()
            .forEach(a -> involvedCountMap.merge(a, 1, Integer::sum));
        }
        
        return (a,b) -> Integer.compare(involvedCountMap.get(a), involvedCountMap.get(b));
    }
}
