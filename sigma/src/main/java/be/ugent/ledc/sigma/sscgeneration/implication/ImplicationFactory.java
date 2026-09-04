package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.core.datastructures.rules.Rule;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import java.util.Set;

/**
 * An interface that is used in the FCF generator to provide a rule implicator
 * that can be used to do rule implication in a particular node of the FCF
 * structure.
 * @author abronsel
 */
public interface ImplicationFactory<R extends Rule<?>>
{
    <T extends Comparable<? super T>> RuleImplicator<T, R> create(String generator, SigmaContractor<T> rules, Set<SigmaRule> contributors);
}
