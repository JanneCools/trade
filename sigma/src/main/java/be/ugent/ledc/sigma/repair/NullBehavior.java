package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import java.util.function.BiFunction;


public enum NullBehavior
{
    NO_REPAIR((rules, a) -> false),
    ALWAYS_REPAIR((rules, a) -> true),
    DOMAIN_REPAIR(SigmaRuleset::hasDomainConstraints),
    CONSTRAINED_REPAIR((rules, a) ->
            rules.hasFiniteDomain(a)
        &&  rules.hasConditionalConstraints(a));
    
    private final BiFunction<SigmaRuleset, String, Boolean> needsRepairWhenNullTest;
    
    private NullBehavior(BiFunction<SigmaRuleset, String, Boolean> needsRepairWhenNullTest)
    {
        this.needsRepairWhenNullTest = needsRepairWhenNullTest;
    }
    
    public boolean repairIfNull(SigmaRuleset rules, String attribute)
    {
        return needsRepairWhenNullTest.apply(rules, attribute);
    }

}
