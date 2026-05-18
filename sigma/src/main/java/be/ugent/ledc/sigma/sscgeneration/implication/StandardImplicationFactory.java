package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.Set;

public class StandardImplicationFactory implements ImplicationFactory
{
    private final RuleImplicator[] implicators = new RuleImplicator[]
    {
        new ConstantImplicator<>(),
        new VariableImplicator<>()
    };
    
    @Override
    public RuleImplicator create(String generator, SigmaContractor<?> contract, Set<SigmaRule> contributors)
    {
        for(RuleImplicator implicator: implicators)
        {
            if(implicator.isApplicable(generator, contract, contributors))
                return implicator;
        }
        
        return null;
    }
}
