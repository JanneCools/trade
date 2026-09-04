package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.Set;

public class StandardImplicationFactory extends SigmaRuleImplicationFactory
{
    private final SigmaRuleImplicator<?>[] implicators = new SigmaRuleImplicator[]
    {
        new ConstantImplicator<>(),
        new VariableImplicator<>()
    };
    
    @Override
    public <T extends Comparable<? super T>> SigmaRuleImplicator<T> create(String generator, SigmaContractor<T> contract, Set<SigmaRule> contributors)
    {
        for(SigmaRuleImplicator<?> implicator: implicators)
        {
            if(implicator.isApplicable(generator, contract, contributors))
                return (SigmaRuleImplicator<T>) implicator;
        }
        
        return null;
    }
}
