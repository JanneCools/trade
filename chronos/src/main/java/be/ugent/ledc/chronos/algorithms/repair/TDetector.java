package be.ugent.ledc.chronos.algorithms.repair;

import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.chronos.rules.TRuleset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects violations of Transition rules in a temporal dataset.
 * @author abronsel
 */
public class TDetector
{
    /**
     * Computes the times at which the temporal dataset contains failures
     * of transition rules and collects the failing rules at those times.

     * @param <I>
     * @param ruleset
     * @param dataset
     * @return 
     */
    public static <I extends Comparable<? super I>> Map<I, Set<SigmaRule>> detectErrors(TRuleset<I> ruleset, TemporalDataset<I> dataset)
    {
        Signal<I, DataObject> oSignal = dataset.getObjectSignal();
 
        I current = oSignal.start();
        
        Map<I, Set<SigmaRule>> failures = new HashMap<>();
        
        while(oSignal.nextIndex(current) != null)
        {
            DataObject o = TRuleset.createObject(
                oSignal.valueAt(current),
                oSignal.valueAfter(current),
                dataset.getContract().getAttributes()
            );

            for(SigmaRule rule: ruleset.getRules())
            {
                boolean satisfied = rule.test(o);
                
                if(!satisfied)
                {
                    if(!failures.containsKey(current))
                    {
                        failures.put(current, new HashSet<>());
                    }
                    
                    failures.get(current).add(rule);
                }
            }
            
            if(failures.containsKey(current))
            {
                System.out.println("  Dirty object: " + oSignal.valueAt(current));
                System.out.println("  Next object: " + oSignal.valueAfter(current));
                System.out.println("");
            }
            
            
            current = oSignal.nextIndex(current);
        }        

        
        return failures;
    }
}
